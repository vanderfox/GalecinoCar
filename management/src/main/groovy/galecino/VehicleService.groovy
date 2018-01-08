package galecino

import com.robo4j.hw.rpi.Servo
import com.robo4j.hw.rpi.i2c.pwm.PCA9685Servo
import com.robo4j.hw.rpi.i2c.pwm.PWMPCA9685Device
import grails.gorm.services.Service

@Service(Vehicle)
abstract class VehicleService {

    private static final int SERVO_FREQUENCY = 50
    private static final int MOTOR_STOPPED = 360
    private static final int MOTOR_FORWARD = 400
    private static final int MOTOR_BACKWARD = 310
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

    void forward() {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(SERVO_FREQUENCY)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(0, MOTOR_FORWARD)

    }

    void backward() {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(SERVO_FREQUENCY)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(0, MOTOR_BACKWARD)
    }

    void left() {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(SERVO_FREQUENCY)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        servo0.setInput(STEERING_LEFT)
    }

    void right() {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(SERVO_FREQUENCY)
        Servo servo0 = new PCA9685Servo(device.getChannel(1))
        servo0.setInput(STEERING_RIGHT)
    }

    void stop() {
        PWMPCA9685Device device = new PWMPCA9685Device()
        device.setPWMFrequency(SERVO_FREQUENCY)
        PWMPCA9685Device.PWMChannel motor0 = device.getChannel(0)
        motor0.setPWM(0, MOTOR_STOPPED)
    }

}
