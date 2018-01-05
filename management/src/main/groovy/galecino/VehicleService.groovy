package galecino

import grails.gorm.services.Service

@Service(Vehicle)
abstract class VehicleService {


    abstract List<Vehicle> list()
    abstract Vehicle save(String name)
    abstract pwmTest() {
           PWMPCA9685Device device = new PWMPCA9685Device()
           PCA9685Servo servo0 = device.getChannel(0)
           device.setPWMFrequency(50)
           servo0.setPWM(0, 205);
           PWMChannel servo0 = device.getChannel(0) 
                    
    }
}
