# GraalVM native with eclair dependencies

Example project showing the usage of a simple akka http server with json serialization, compiled with GraalVM native-image.
The goal of the project is to use all of eclair's dependencies to prepare moving to the graal platform.

## Pre-requisites
  * Maven 3.6.x
  * [GraalVM](https://github.com/oracle/graal/releases) version 19.2.0
  * `native-image` from `$GRAAL_HOME/bin` in `PATH`
  
Suggested environment variables:

    export GRAAL_HOME=/path/to/your/graal/installation/graalvm-ce-19.2.0
    export PATH=$PATH:$GRAAL_HOME/bin
    
[Install native-image](https://www.graalvm.org/docs/reference-manual/aot-compilation/#install-native-image):

    gu install native-image
  
This project needs `bitcoin-lib` v0.17-SNAPSHOT which is not currently published, in order to build it you first 
need to build its dependency [`secp256k1-jni`](https://github.com/araspitzu/secp256k1/tree/jni_non_static_init/src/java). 
Once you successfully installed the artifact in your local maven repo then you can build the correct
version of [`bitcoin-lib`](https://github.com/araspitzu/bitcoin-lib/tree/new_jni), this is a prerequisite for building this 
project.
  
## Compiling
    
    JAVA_HOME=$GRAAL_HOME mvn clean package
    
It might take a few minutes to compile.
   
## Running
        
    # Linux:
    target/graal-akka-maven -Djava.library.path=$JAVA_HOME/lib

Because the project is compiled with
[Java Crypto enabled](https://github.com/oracle/graal/blob/master/substratevm/JCA-SECURITY-SERVICES.md)
for the native image (to support HTTPS) `java.library.path` system property must be set at runtime
to point to a directory where the dynamic library for the SunEC provider is located.

## What works
- Akka-actor 2.5
- Akka-http 10.1.8 (see `/json` endpoint for a demo)
- Logging
- Typesafe config
- Json4s-jackson (no reflection though) (see `/json` endpoint for a demo)
- JHeaps (see `/jheaps` endpoint for a demo)
- Scodec (see `/scodec` endpoint for a demo)
- Apache commons codec (see `/commons` endpoint for a demo)
- SQLite 3.27.2.1 with JNI (see `/query?name=Hal` endpoint for a demo)
- bitcoin-lib with JNI (see `/bitcoinlib` endpoint for a demo)
- Guava 24-android (see `/hostandport` endpoint)

## What doesn't work
- Sttp with OkHttpBackend doesn't work, there is an issue https://github.com/oracle/graal/issues/1521
  where users claim that using a particular version of OkHttp does work, however sttp is built against a different version.
    
## Missing libraries from eclair-core
- JeroMQ

## How it works
Most of the graal-specific configuration for `native-image` in the `graal-config` folder.The akka-specific configuration 
is provided by [akka-graal-config](https://github.com/vmencik/akka-graal-config) repository which publishes a set of 
jar artifacts that contain the necessary configuration resources for `native-image` to compile Akka modules. 
Just having these jars in the classpath is enough for `native-image` to pick up this configuration.
See [this blog post](https://medium.com/graalvm/simplifying-native-image-generation-with-maven-plugin-and-embeddable-configuration-d5b283b92f57)
for more details on how that mechanism works.

### Reflection configuration
See [SubstrateVM docs](https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md)
for details.

Configuration for Akka itself is provided by [akka-graal-config](https://github.com/vmencik/akka-graal-config)
dependencies. This repo contains only reflection configuration to get java.util.logging working.

Note however that reflective access to `context` and `self` fields must be configured for every actor
that is monitored with `context.watch` (observed empirically).
Otherwise you'll get [an error from Akka's machinery](https://github.com/akka/akka/blob/v2.5.21/akka-actor/src/main/scala/akka/actor/ActorCell.scala#L711).

### HTTPS
Passing the `--enable-url-protocols=https` option to `native-image` enables JCE features.
This configuration option is enabled by configuration from `graal-akka-http` dependency.

### Affinity Pool
`akka.dispatch.affinity.AffinityPool` is using MethodHandles which causes errors like this one
during native image build:

    Error: com.oracle.graal.pointsto.constraints.UnsupportedFeatureException: Invoke with MethodHandle argument could not be reduced to at most a single call: java.lang.invoke.MethodHandle.bindTo(Object)
    
The workaround is to initialize affected classes (and we actually do this for the whole classpath)
at build time using the `--initalize-at-build-time` option.

### Bitcoin-lib
[bitcoin-lib](https://github.com/ACINQ/bitcoin-lib) is a library for bitcoin primitives by ACINQ, it uses the native 
libsecp256k1 to perform EC operations and that is wrapped in a JNI layer. To use bitcoin-lib with graal's native-image
you need to build a particular version of the JNI (the standard ones don't work because of the static initialization), 
follow the instructions [here](https://github.com/araspitzu/secp256k1/tree/jni_non_static_init/src/java) to build 
`secp256k1-jni`. Only after you published locally `secp256k1-jni` you can build the required version of bitcoin-lib
from [here](https://github.com/araspitzu/bitcoin-lib/tree/new_jni).

### Lightbend Config
Static initializers of `com.typesafe.config.impl.ConfigImpl$EnvVariablesHolder`
and `com.typesafe.config.impl.ConfigImpl$SystemPropertiesHolder` need to be run at runtime using
the `--initialize-at-run-time` option.
Otherwise the environment from image build time will be baked in to the configuration.

### Logging
It is currently not easy to get Logback working because of its Groovy dependencies and incomplete
classpath problems with `native-image` so `java.util.logging` is used instead.


