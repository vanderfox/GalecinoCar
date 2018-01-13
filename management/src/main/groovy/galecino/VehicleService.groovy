package galecino

import com.robo4j.hw.rpi.Servo
import com.robo4j.hw.rpi.i2c.pwm.PCA9685Servo
import com.robo4j.hw.rpi.i2c.pwm.PWMPCA9685Device
import com.robo4j.hw.rpi.pwm.PWMServo
import grails.gorm.services.Service

@Service(Vehicle)
abstract class VehicleService {

    private static final int SERVO_FREQUENCY = 50
    private static final int MOTOR_BACKWARD = 360
    private static final int MOTOR_FORWARD = 400
    private static final int MOTOR_STOPPED = 310
    private static final int STEERING_LEFT = 420
    private static final int STEERING_RIGHT = 310

    abstract List<Vehicle> list()
    abstract Vehicle save(String name)
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
        motor0.setPWM(on, off)

    }

    void backward(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_BACKWARDD) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(on, off)
    }


    void steer(float angle) {
        // seems like 360 right 520 left
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(50)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        //Servo servo0 = new PWMServo()
        if (angle == 0) {
            angle = 0.3
        }
        servo0.setInput(angle)
    }

    void stop(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_STOP) {
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
            350, 300)
        } else {
            pulse = map_range(throttle,
                    0, 1,
                    490, 350)
        }
        // set throttle
        forward(50,pulse)


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


}
