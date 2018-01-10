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

    void left(int frequency = SERVO_FREQUENCY, int input = STEERING_LEFT) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        servo0.setInverted(true)
        servo0.setInput(input)
    }


    void leftAngle(int angle) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        servo0.setInverted(true)
        servo0.setInput(input)
    }

    void steer(float angle) {
        // seems like 360 right 520 left
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(50)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        //Servo servo0 = new PWMServo()

        servo0.setInput(angle)
    }

    void right(int frequency = SERVO_FREQUENCY, int input = STEERING_RIGHT) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        servo0.setInput(input)
    }

    void stop(int frequency = SERVO_FREQUENCY, int on = 0, int off = MOTOR_STOP) {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(frequency)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(on, off)
    }

}
