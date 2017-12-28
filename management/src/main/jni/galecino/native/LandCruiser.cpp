#include <stdio.h>
#include <pigpio.h>

#include "ESC.h"

using namespace std;

JNIEXPORT int JNICALL Java_ESC_gasPedal
(JNIEnv *env, jobject o, jint pin, jint hertz, jint dutyCycle) {
    printf("pin = %d, hertz= %d, dutyCycle= %d \n", pin, hertz, dutyCycle);

    if (gpioInitialise() < 0) {
        printf("failed gpioInitialise \n");
        return 0;
    }

    gpioHardwarePWM(pin, hertz, dutyCycle);

    return 1;
}

JNIEXPORT int JNICALL Java_Steering_turnWheels
(JNIEnv *env, jobject o, jint pin, jint pulseWidth) {

    printf("pin = %d, pulseWidth= %d \n", pin, pulseWidth);

    if (gpioInitialise() < 0) {
        printf("failed gpioInitialise \n");
        return 0;
    }
    gpioServo(pin, pulseWidth);

    return 1;
}