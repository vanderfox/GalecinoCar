package galecino

import grails.gorm.services.Service

@Service(Vehicle)
interface VehicleService {
    List<Vehicle> list()
    Vehicle save(String name)
}
