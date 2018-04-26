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

    private static final int SERVO_FREQUENCY = 20
    private static final int MOTOR_BACKWARD = 674
    private static final int MOTOR_FORWARD = 755
    private static final int MOTOR_STOPPED = 700
    private static final int STEERING_LEFT = 420
    private static final float STEERING_STRAIGHT_ANGLE = 44.0
    private static final int STEERING_RIGHT = 360
    private static final int MAX_THROTTLE_FORWORD = 3500
    private static final int MIN_THROTTLE_FORWORD = 755
    private static final int MAX_THROTTLE_BACKWARD = 2
    private static final int MIN_THROTTLE_BACKWARD = 674
    @Value('${galecino.servo.trim:0.0}')
    protected float configTrim
    @Value('${galecino.pwmFrequency:50}')
    protected int pwmFrequency

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

    @PostConstruct
    void init() {
        delayThread = Executors.newScheduledThreadPool(1)
        commands = new ArrayBlockingQueue(100)
        startDriveThread()
    }

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
            //servo1.setInput(-1);
            motor0.setPWM(0, MOTOR_FORWARD);
            //motor1.setPWM(0, MOTOR_FORWARD);
            Thread.sleep(500);
            servo0.setInput(1);;
            //servo1.setInput(1);;
            motor0.setPWM(0, MOTOR_BACKWARD);
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
        System.out.println("frequency:"+frequency+" on:"+on+" off:"+off)
        motor0.setPWM(on, off)

    }

    void backward(int frequency = pwmFrequency, int on = 0, int off = MOTOR_BACKWARDD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(on, off)
    }


    void steer(float angle, float trim = 0.0) {
        // seems like 360 right 520 left
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(pwmFrequency)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        System.out.println("steer angle non corrected:${angle} trim:${trim}")
        servo0.setInput(STEERING_STRAIGHT_ANGLE+angle)
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
        int duration = 0
        // set steering
        LOG.info("drivemode="+driveMode)
        LOG.info("drivethread="+th+" status:"+th.state+" isalive:"+th.isAlive())
        if (driveMode == "user") {
            LOG.info("delayThread="+delayThread)
            if (th && th.state == Thread.State.TERMINATED) {
                startDriveThread()
            }
            delayThread.schedule({
                LOG.info("command queued")
                commands.put([direction:direction, duration:duration, angle:angle, throttle:throttle])
            } as Runnable, delay, TimeUnit.MILLISECONDS)
        } else if (driveMode == "pilot") {
            //stop all remote control and reset motors?
            if (!process || !process.alive) {
                //stop all remote control and reset motors in case car is moving
                stop()
                Process process = "python galenciocar.py".execute()
            }
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
        if (!piCamera) {
            synchronized(this) {
                long startTime = System.currentTimeMillis()
                piCamera = new RPiCamera()
                piCamera.setAWB(AWB.AUTO) 	    // Change Automatic White Balance setting to automatic
                        .setTimeout(30)		    // Wait 1 second to take the image
                        .setBrightness(60)
                        .turnOffPreview()            // Turn on image preview
                        .setEncoding(Encoding.JPG) //
                long endTime = System.currentTimeMillis()
                System.out.println("init camera took ${endTime-startTime}ms")
            }
        }
        long startTime = System.currentTimeMillis()

        BufferedImage image = piCamera.takeBufferedStill(160,120)
        long endTime = System.currentTimeMillis()
        System.out.println("camera pic took ${endTime-startTime}ms")
        startTime = System.currentTimeMillis()
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        if (!image) {
          image = piCamera.takeBufferedStill(160,120)
	}
        if (image) {
        ImageIO.write(image, "jpg", baos)
        byte[] imageOut = baos.toByteArray()
        endTime = System.currentTimeMillis()
        System.out.println("pic jpg convert took ${endTime-startTime}ms")
        imageOut
        } else {
          null
        }

    }



}
