micronaut.server.executors.io.type = "fixed"
micronaut.server.executors.io.nThreads = 75
galecino.servo.trim = 0.3
micronaut.server.port = "8887"
micronaut.server.cors.enabled = true
hibernate {
    hbm2ddl {
        auto = "create-drop"
    }
}
router.staticResources.default.enabled = true
router.staticResources.default.mapping = "/static/**"
router.staticResources.default.paths = "classpath:static"
galecino.pwmFrequency = 20
