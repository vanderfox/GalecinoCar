/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package galecino

import com.hopding.jrpicam.RPiCamera
import grails.gorm.transactions.Transactional
import org.particleframework.context.annotation.Value
import org.particleframework.http.HttpResponse
import org.particleframework.http.MediaType
import org.particleframework.http.annotation.Controller
import org.particleframework.web.router.annotation.Get
import org.particleframework.web.router.annotation.Post

import javax.annotation.PostConstruct
import javax.inject.Inject
import javax.inject.Singleton
import java.awt.image.BufferedImage
import org.particleframework.http.MediaType

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Controller("/")
@Singleton
class VehicleController {

    @Inject
    VehicleService vehicleService

    @Value('galecino.servo.trim:0.0')
    protected float configTrim

    List<Vehicle> index() {
        vehicleService.list()
        vehicleService.pwmTest()
    }

    @Transactional
    @PostConstruct
    void setup() {
        vehicleService.save 'VanderfoxCar'
    }


    byte[] takeStill() {
        RPiCamera piCamera = new RPiCamera("/home/pi/Pictures")
        BufferedImage image = piCamera.takeBufferedStill()
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        return baos.toByteArray()

    }


    @Get("/video")
    HttpResponse<byte[]> video() {
        byte[] image = takeStill()
        File imageFile = new File("${System.currentTimeMillis()}.jpg")

        imageFile.withDataOutputStream { out ->
            out.write(image)
        }

        return HttpResponse.ok(image).header("Content-type","multipart/x-mixed-replace;boundary=--boundarydonotcross")
    }


    @Get("/pwmTest")
    HttpResponse<String> pwmTest() {
        vehicleService.pwmTest()
        return HttpResponse.ok("Car Test started in endless loop")

    }

    @Get
    HttpResponse<String> forward(int frequency, int on, int off) {
       vehicleService.forward(frequency,on,off)
       return HttpResponse.ok("forward")
    }

    @Get
    HttpResponse<String> backward(int frequency, int on, int off) {
        vehicleService.backward(frequency, on, off)
        return HttpResponse.ok("backward")
    }

    @Get
    HttpResponse<String> stop(int frequency, int on, int off) {
        vehicleService.stop(frequency,on,off)
        return HttpResponse.ok("stop")
    }

    @Get("/steer")
    HttpResponse<String> steer(float angle, float trim) {
        System.out.println("configTrim in controller=${configTrim}")
        vehicleService.steer(angle,trim)
        return HttpResponse.ok("angle:${angle}")
    }


    @Post(consumes = MediaType.APPLICATION_FORM_URLENCODED)
    HttpResponse<String> drive(float angle, float throttle, String drive_mode = "user", Boolean recording = false) {
        //vehicleService.steer(angle)
        System.out.println("drive called")
        vehicleService.drive(angle,throttle)
        return HttpResponse.ok("angle:${angle} throttle:${throttle}")
    }
}
