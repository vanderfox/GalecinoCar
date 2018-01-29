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
import org.particleframework.context.annotation.Value

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Service(Vehicle)
abstract class VehicleService {

    private static final int SERVO_FREQUENCY = 50
    private static final int MOTOR_BACKWARD = 360
    private static final int MOTOR_FORWARD = 400
    private static final int MOTOR_STOPPED = 310
    private static final int STEERING_LEFT = 420
    private static final int STEERING_RIGHT = 310
    private static final int MAX_THROTTLE_FORWORD = 600
    private static final int MIN_THROTTLE_FORWORD = 400 
    private static final int MAX_THROTTLE_BACKWARD = 1
    private static final int MIN_THROTTLE_BACKWARD = 310 
    @Value('galecino.servo.trim:0.0')
    protected float configTrim

    abstract List<Vehicle> list()
    abstract Vehicle save(String name)

    RPiCamera piCamera

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

    void forward(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_FORWARD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        System.out.println("")
        motor0.setPWM(on, off)

    }

    void backward(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_BACKWARDD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(on, off)
    }


    void steer(float angle, float trim = 0.0) {
        // seems like 360 right 520 left
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(50)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        servo0.setInput(angle)
        System.out.println("configTrim in service=${configTrim}")
        if (trim == 0) {
           trim = configTrim
        }
        servo0.setTrim(trim)
        System.out.println("steer angle:${angle} trim:${trim}")

    }

    void stop(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_STOPPED) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(on, off)
    }

    void drive(float angle, float throttle, String driveMode = "user", Boolean recording = false) {
        // set steering
        steer(angle)
        int pulse = 0
        if (throttle > 0) {
            pulse = map_range(throttle,
            0, 1,
            MIN_THROTTLE_FORWORD, MAX_THROTTLE_FORWORD)
            System.out.println("fwd Pulse=${pulse}")
        } else {
            pulse = map_range(throttle,
                    -1, 0,
                    MAX_THROTTLE_BACKWARD, MIN_THROTTLE_BACKWARD)
            System.out.println("backwd  Pulse=${pulse}")
        }
        // set throttle
        forward(50,0,pulse)


    }

    private float map_range(x, X_min, X_max, Y_min, Y_max) {
    //    '''
    //Linear mapping between two ranges of values
    //'''
        float X_range = X_max - X_min
        float Y_range = Y_max - Y_min
        float XY_ratio = X_range / Y_range

        int y = ((x - X_min) / XY_ratio + Y_min) // 1

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
        ImageIO.write(image, "jpg", baos)
        byte[] imageOut = baos.toByteArray()
        endTime = System.currentTimeMillis()
        System.out.println("pic jpg convert took ${endTime-startTime}ms")
        imageOut

    }



}
