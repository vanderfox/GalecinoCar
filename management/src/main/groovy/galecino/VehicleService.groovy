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
    private static final int STEERING_LEFT = 420
    private static final float STEERING_STRAIGHT_ANGLE = 44.0
    private static final int STEERING_RIGHT = 360
    private static final int MAX_THROTTLE_FORWORD = 1000
    private static final int MIN_THROTTLE_FORWORD = 800
    private static final int MAX_THROTTLE_BACKWARD = 500
    private static final int MIN_THROTTLE_BACKWARD = 730
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

    private void startDriveThread() {

        th = Thread.start {
            try {
                LOG.info("inside thread")
                /*if (!commands && !delayThread) {
                    //this is a workaround because @PostConstrct seems to have stopped working
                    init()
                }*/
                while (running) {
                    def recent = []

                    commands.drainTo(recent)
                    LOG.info("recent="+recent)

                    if (recent.size()) {
                        LOG.info recent.size().toString()
                        def command = recent[-1]
                        float throttle = command.throttle
                        LOG.info command.direction
                        def duration = command.duration

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
    void pwmTest() {
        System.out.println("Creating device...");
        PWMPCA9685Device device = new PWMPCA9685Device();
        device.setPWMFrequency(SERVO_FREQUENCY);
        Servo servo0 = new PCA9685Servo(device.getChannel(1));
        //Servo servo1 = new PCA9685Servo(device.getChannel(0));
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0);
        //PWMPCA9685Device.PWMChannel motor1 = device.getChannel(3);

        System.out.println("Setting start conditions...");
        servo0.setInput(0);
        ///servo1.setInput(0);
        motor0.setPWM(0, MOTOR_STOPPED);
        //motor1.setPWM(0, MOTOR_STOPPED);

        System.out.println("Press enter to run loop!");
        System.in.read();
        System.out.println("Running perpetual loop...");
        while (true) {
            servo0.setInput(-1);
            Thread.sleep(500);
            servo0.setInput(1);
            //servo1.setInput(-1);
            motor0.setPWM(0, 800);
            //motor1.setPWM(0, MOTOR_FORWARD);
            Thread.sleep(500);
            servo0.setInput(1);;
            //servo1.setInput(1);;
            motor0.setPWM(0, 200);
            //motor1.setPWM(0, MOTOR_BACKWARD);
            Thread.sleep(500);
            servo0.setInput(0);
            //servo1.setInput(0);
            motor0.setPWM(0, MOTOR_STOPPED);
            //motor1.setPWM(0, MOTOR_STOPPED);
            Thread.sleep(1000);
        }
    }

    void forward(int frequency = pwmFrequency, int on = 0, int off = MOTOR_FORWARD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        LOG.info("fwd frequency:"+frequency+" on:"+on+" off:"+off)
/**
        if (pwmFrequency < MOTOR_STOPPED) {
            //for backward we have to do a dance
            motor0.setPWM(on,MOTOR_BACKWARD)
            Thread.sleep(100)
            motor0.setPWM(on,MOTOR_STOPPED)
            Thread.sleep(100)
            motor0.setPWM(on,pwmFrequency)
        } else {
            motor0.setPWM(on, off)
        }**/
        motor0.setPWM(on,off)
        Thread.sleep(1000)

    }
    void initThrottle(int frequency = pwmFrequency, int on = 0, int off = MOTOR_STOPPED) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        LOG.info("init motor frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on, off)

    }

    void backward(int frequency = pwmFrequency, int on = 0, int off = MOTOR_BACKWARDD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        LOG.info("backward frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on, off)
    }


    void steer(float angle, float trim = 0.0) {
        // seems like 360 right 520 left
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(pwmFrequency)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        System.out.println("steer angle non corrected:${angle} trim:${trim}")
        servo0.setInput((angle).toFloat())
        System.out.println("configTrim in service=${configTrim}")
        if (trim == 0) {
           trim = configTrim
        }
        servo0.setTrim(trim)
        System.out.println("corrected steer angle:${angle} trim:${trim}")

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
                stop()
                if (th) {
                    th.stop()
                }
                if (delayThread) {
                    delayThread.shutdownNow()
                }
                autopilotThread = "python /home/pi/d2/galenciocar.py --model /home/pi/d2/models/smartpilot".execute()
                LOG.info("Autopilot started:"+autopilotThread.toString())
                UNIXProcess.ProcessPipeInputStream oStream = autopilotThread.errorStream
                List<String> errors = oStream.readLines()
                LOG.info("Error stream: $errors")
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

    float map_range(float x, int X_min, int X_max, int Y_min, int Y_max) {
    //    '''
    //Linear mapping between two ranges of values
    //'''
        float X_range = X_max - X_min
        float Y_range = Y_max - Y_min
        float XY_ratio = X_range / Y_range

        int y = (((x - X_min) / XY_ratio + Y_min)).toInteger() // 1

        return y
    }


    byte[] takeStill() {
        if (currentDriveMode == "user") {
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
            if (autopilotThread) {
                LOG.info("autopilot thread alive=${autopilotThread.alive} exitValue=${autopilotThread.exitValue()}")
            }
        }

    }



}
