package galecino

import com.hopding.jrpicam.RPiCamera
import com.hopding.jrpicam.enums.AWB
import com.hopding.jrpicam.enums.DRC
import com.hopding.jrpicam.enums.Encoding
import com.robo4j.hw.rpi.Servo
import com.robo4j.hw.rpi.i2c.pwm.PCA9685Servo
import com.robo4j.hw.rpi.i2c.pwm.PWMPCA9685Device
import com.robo4j.hw.rpi.pwm.PWMServo
import grails.gorm.services.Service
import io.micronaut.context.annotation.Value
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service(Vehicle)
abstract class VehicleService {

    //these vales seem double the donkeycar pwm vals
    private static final int SERVO_FREQUENCY = 20
    private static final int MOTOR_BACKWARD = 730
    private static final int MOTOR_FORWARD = 800
    private static final int MOTOR_STOPPED = 760
    private static final int STEERING_LEFT = 880
    private static final float STEERING_STRAIGHT_ANGLE = 44.0
    private static final int STEERING_RIGHT = 570
    private static final int MAX_THROTTLE_FORWORD = 1000
    private static final int MIN_THROTTLE_FORWORD = 800
    private static final int MAX_THROTTLE_BACKWARD = 610
    private static final int MIN_THROTTLE_BACKWARD = 715
    @Value('${galecino.servo.trim:0.0}')
    protected float configTrim
    @Value('${galecino.pwmFrequency:20}')
    protected int pwmFrequency
    Process autopilotThread

    abstract List<Vehicle> list()
    abstract Vehicle save(String name)

    RPiCamera piCamera
    Process process // this is the process python is running in pilot mode
    ArrayBlockingQueue commands
    def running = true
    def delay = 50
    Thread th
    ScheduledThreadPoolExecutor delayThread
    protected static final Logger LOG = LoggerFactory.getLogger(VehicleService.class);
    String currentDriveMode = "user"

    @PostConstruct
    void init() {
        LOG.info("Init thread started")
        delayThread = Executors.newScheduledThreadPool(1)
        commands = new ArrayBlockingQueue(100)
        initThrottle(20,0,MOTOR_FORWARD) // make sure motor is ready
        Thread.sleep(100)
        initThrottle(20,0,MOTOR_STOPPED)
        Thread.sleep(100)
        startDriveThread()
        LOG.info("Init thread finished")
    }

    /**
     * this is the main drive thread which contains a queue to avoid overwhelming the car with requests
     */
    private void startDriveThread() {

        th = Thread.start {
            try {
                LOG.info("inside thread")
                while (running) {
                    def recent = []

                    commands.drainTo(recent)
                    LOG.info("recent="+recent)

                    if (recent.size()) {
                        LOG.info recent.size().toString()
                        def command = recent[-1]
                        float throttle = command.throttle
                        LOG.info command.direction
                        switch (command.direction) {
                            case 'forward':
                                if (process && process?.alive) {
                                    process.destroyForcibly()
                                }
                                steer(command.angle)
                                int pulse = 0
                                if (throttle > 0) {
                                    pulse = map_range(throttle,
                                            0, 1,
                                            MIN_THROTTLE_FORWORD, MAX_THROTTLE_FORWORD)
                                    LOG.info("fwd Pulse=${pulse} throttle:"+throttle)
                                } else {
                                    if (throttle < 0) {
                                        pulse = map_range(throttle,
                                                -1, 0,
                                                MAX_THROTTLE_BACKWARD, MIN_THROTTLE_BACKWARD)
                                        LOG.info("backwd  Pulse=${pulse} throttle:"+throttle)
                                    }
                                    if (throttle == 0) {
                                       LOG.info("stop")
                                        stop(pwmFrequency)
                                        return
                                    }
                                }
                                // set throttle
                                forward(pwmFrequency, 0, pulse)
                                break
                            case 'stop':
                                stop(pwmFrequency)
                                break
                        }
                        if (commands.size() == 0) {
                            stop(pwmFrequency)
                        }
                    }
                    if (commands.size() == 0) {
                        Thread.sleep(100)
                    }
                }
            } catch (Exception e) {
                LOG.error("drive thread crashed:"+e.message,e)
            }
        }
    }

    /**
     * test method to make sure things are connected properly
     */
    void pwmTest() {
        System.out.println("Creating device...");
        PWMPCA9685Device device = new PWMPCA9685Device();
        device.setPWMFrequency(SERVO_FREQUENCY);
        Servo servo0 = new PCA9685Servo(device.getChannel(1));
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0);
        System.out.println("Setting start conditions...");
        servo0.setInput(0);
        motor0.setPWM(0, MOTOR_STOPPED);

        System.out.println("Press enter to run loop!");
        System.in.read();
        System.out.println("Running perpetual loop...");
        while (true) {
            servo0.setInput(-1);
            Thread.sleep(500);
            servo0.setInput(1);
            motor0.setPWM(0, 800);
            Thread.sleep(500);
            servo0.setInput(1);;
            motor0.setPWM(0, 200);
            Thread.sleep(500);
            servo0.setInput(0);
            motor0.setPWM(0, MOTOR_STOPPED);
            Thread.sleep(1000);
        }
    }

    /**
     * controls the throttle on the car for forward and backward (based on pwm values
     * @param frequency
     * @param on
     * @param off
     */
    void forward(int frequency = pwmFrequency, int on = 0, int off = MOTOR_FORWARD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        LOG.info("fwd frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on,off)
        Thread.sleep(1000) // this is important or the motor doesn't have time to respond
    }

    /**
     * this makes the motors 'wake up' with special pwm values.
     * @param frequency
     * @param on
     * @param off
     */
    void initThrottle(int frequency = pwmFrequency, int on = 0, int off = MOTOR_STOPPED) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        LOG.info("init motor frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on, off)
    }

    /**
     * make steering servo 'wake up' with special pwm values.
     * @param frequency
     * @param on
     * @param off
     */
    void initSteering(int frequency = pwmFrequency, int on = 0, int off = 580) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(1)
        LOG.info("init motor frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on, off)
    }

    /**
     * This isn't used in drive mode but for diagnostics via controller
     * @param frequency
     * @param on
     * @param off
     */
    void backward(int frequency = pwmFrequency, int on = 0, int off = MOTOR_BACKWARDD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        LOG.info("backward frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on, off)
    }

    /**
     * controls steering of the car
     * @param angle
     * @param trim
     */
    void steer(float angle, float trim = 0.0) {
        // seems like 360 right 520 left
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(50) //internetz says 50 for servos is the shiz
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        LOG.info("steer angle non corrected:${angle} trim:${trim}")
        if (trim != 0) {
            trim = configTrim
            servo0.setTrim(trim)
        }
        servo0.setInput((angle).toFloat())
        System.out.println("configTrim in service=${configTrim}")
        Thread.sleep(1000) // impor

    }

    void stop(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_STOPPED) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(on, off)
    }

    void driveScheduled(float angle, float throttle, String driveMode = "user", Boolean recording = false) {
        String direction = "forward"
        currentDriveMode = driveMode
        int duration = 0
        // set steering
        LOG.info("drivemode="+driveMode)
        LOG.info("drivethread="+th+" status:"+th?.state+" isalive:"+th?.isAlive())
        if (driveMode == "user") {
            if (autopilotThread) {
                autopilotThread.destroyForcibly()
            }
            LOG.info("delayThread="+delayThread)
            if ((th && th.state == Thread.State.TERMINATED) || !th) {
                startDriveThread()
            }
            if (delayThread && delayThread.terminated) {
                init() // reset state
            }
            delayThread.schedule({
                LOG.info("command queued")
                commands.put([direction:direction, duration:duration, angle:angle, throttle:throttle])
            } as Runnable, delay, TimeUnit.MILLISECONDS)
        } else if (driveMode == "local") {
            //stop all remote control and reset motors?
                //stop all remote control and reset motors in case car is moving
                if (piCamera) {

                    piCamera.turnOffPreview()
                    piCamera.stop()

                    Thread.sleep(500)
                    //piCamera = null
                }
                stop()
                if (th) {
                    th.stop()
                }
                if (delayThread) {
                    delayThread.shutdownNow()
                }
                def out = new StringBuffer()
		        def err = new StringBuffer()
                autopilotThread = "python /home/pi/d2/galencino.py --model /home/pi/d2/models/smartpilot".execute()
                autopilotThread.consumeProcessOutput( out, err )
                autopilotThread.waitFor()
                if( out.size() > 0 ) LOG.info out.toString()
                if( err.size() > 0 ) LOG.info err.toString()
                LOG.info("Autopilot started:"+autopilotThread.toString())
        }

    }

    void drive(float angle, float throttle, String driveMode = "user", Boolean recording = false) {
        // set steering
        if (driveMode == "user") {
            if (process && process?.alive) {
                process.destroyForcibly()
            }
            steer(angle)
            int pulse = 0
            if (throttle > 0) {
                pulse = map_range(throttle,
                        0, 1,
                        MIN_THROTTLE_FORWORD, MAX_THROTTLE_FORWORD)
                System.out.println("fwd Pulse=${pulse} throttle:"+throttle)
            } else {
                if (throttle < 0) {
                    pulse = map_range(throttle,
                            -1, 0,
                            MAX_THROTTLE_BACKWARD, MIN_THROTTLE_BACKWARD)
                    System.out.println("backwd  Pulse=${pulse} throttle:"+throttle)
                }
                if (throttle == 0) {
                    System.out.println("stop")
                    stop(pwmFrequency)
                    return
                }
            }
            // set throttle
            forward(pwmFrequency, 0, pulse)
        } else if (driveMode == "pilot") {
            //stop all remote control and reset motors?
            if (!process || !process.alive) {
                //stop all remote control and reset motors in case car is moving
                stop()
                Process process = "python galenciocar.py".execute()
            }
        }

    }

    /**
     * Linear mapping between two ranges of values
     * @param x
     * @param X_min
     * @param X_max
     * @param Y_min
     * @param Y_max
     * @return
     */
    float map_range(float x, int X_min, int X_max, int Y_min, int Y_max) {

        float X_range = X_max - X_min
        float Y_range = Y_max - Y_min
        float XY_ratio = X_range / Y_range

        int y = (((x - X_min) / XY_ratio + Y_min)).toInteger() // 1

        return y
    }

    /**
     * takes a still image from camera
     * In the future we can keep a thread open off the video stream and slice off stills from it for
     * more real time performance, this is because the camera can only take pics every 750ms
     * @return array of bytes which is the image
     */
    byte[] takeStill() {
        if (!autopilotThread || !autopilotThread.alive) {
            if (!piCamera) {
                synchronized (this) {
                    long startTime = System.currentTimeMillis()
                    piCamera = new RPiCamera()
                    piCamera.setAWB(AWB.AUTO)        // Change Automatic White Balance setting to automatic
                            .setTimeout(30)            // Wait 1 second to take the image
                            .setBrightness(60)
                            .turnOffPreview()            // Turn on image preview
                            .setEncoding(Encoding.JPG) //
                    long endTime = System.currentTimeMillis()
                    System.out.println("init camera took ${endTime - startTime}ms")
                }

            long startTime = System.currentTimeMillis()

            BufferedImage image = piCamera.takeBufferedStill(160, 120)
            long endTime = System.currentTimeMillis()
            System.out.println("camera pic took ${endTime - startTime}ms")
            startTime = System.currentTimeMillis()
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            if (!image) {
                image = piCamera.takeBufferedStill(160, 120)
            }
            if (image) {
                ImageIO.write(image, "jpg", baos)
                byte[] imageOut = baos.toByteArray()
                endTime = System.currentTimeMillis()
                System.out.println("pic jpg convert took ${endTime - startTime}ms")
                imageOut
            } else {
                null
            }
            } else {
                piCamera.stop()
                piCamera = null
            }
        } else {
            if (autopilotThread) {
                LOG.info("autopilot thread alive=${autopilotThread.alive}")
            }
        }

    }

}
