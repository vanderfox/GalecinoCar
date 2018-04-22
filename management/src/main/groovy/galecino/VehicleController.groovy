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
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher

import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Singleton
import java.awt.image.BufferedImage
import io.micronaut.http.MediaType

import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@Controller("/")
@Singleton
class VehicleController {

    @Inject
    VehicleService vehicleService

    @Value('${galecino.servo.trim:0.0}')
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
       return vehicleService.takeStill()
    }


    @Get(produces = "image/jpeg")
    HttpResponse<byte[]> video() {
        byte[] image = takeStill()
        System.out.println("Image size="+image?.size())
        //File imageFile = new File("${System.currentTimeMillis()}.jpg")

        //imageFile.withDataOutputStream { out ->
        //    out.write(image)
        //}
        //ByteBuffer byteBuffer = ByteBuffer.wrap(image)
        if (!image) {
            image = takeStill() //retry sometimes its null	
	    }
        return HttpResponse.ok(image).header("Content-type","multipart/x-mixed-replace;boundary=--boundarydonotcross")

       //return image
    }



    @Get(produces = "image/jpeg")
    Publisher<HttpResponse> videoRx() {
        //byte[] image = takeStill()
        System.out.println("Image size="+image?.size())
        //File imageFile = new File("${System.currentTimeMillis()}.jpg")

        //imageFile.withDataOutputStream { out ->
        //    out.write(image)
        //}
        //ByteBuffer byteBuffer = ByteBuffer.wrap(image)
        //if (!image) {
        //    image = takeStill() //retry sometimes its null
        //}
        Flowable<byte[]> stillFlow = Flowable.fromArray(takeStill())

        return Flowable.just(HttpResponse.ok(Single.fromPublisher(stillFlow).blockingGet()).header("Content-type","multipart/x-mixed-replace;boundary=--boundarydonotcross"))

        //return image
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


    @Get(produces = 'text/html')
    HttpResponse<String> drive(float angle, float throttle, String drive_mode = "user", Boolean recording = false) {
        //vehicleService.steer(angle)
        System.out.println("drive called")
        vehicleService.driveScheduled(angle,throttle)
        //vehicleService.drive(angle,throttle)
        return HttpResponse.ok("angle:${angle} throttle:${throttle}")
    }
}
