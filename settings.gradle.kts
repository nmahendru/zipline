rootProject.name = "zipline-root"

include(":zipline")
include(":zipline:testing")
include(":zipline-cli")
include(":zipline-gradle-plugin")
include(":zipline-kotlin-plugin")
include(":zipline-kotlin-plugin:tests")
include(":zipline-bytecode")
include(":zipline-loader")
include(":zipline-loader-testing")
include(":zipline-profiler")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
