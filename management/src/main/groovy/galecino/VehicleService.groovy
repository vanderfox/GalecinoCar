package galecino

import com.robo4j.hw.rpi.i2c.pwm.PCA9685Servo
import com.robo4j.hw.rpi.i2c.pwm.PWMPCA9685Device
import grails.gorm.services.Service

@Service(Vehicle)
abstract class VehicleService {


    abstract List<Vehicle> list()
    abstract Vehicle save(String name)
    void pwmTest() {
           PWMPCA9685Device device = new PWMPCA9685Device()
           PCA9685Servo servo0 = device.getChannel(0)
           device.setPWMFrequency(50)
           servo0.setPWM(0, 205)

    }
}
