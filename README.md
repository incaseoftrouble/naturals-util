# naturals-util

Naturals-util is a collection of data-structures and utility methods for dealing with natural numbers, especially with the `{0, ..., n}` domain.
This situation often arises when dealing with indexed or enumerated structures, for example transition systems with enumerated states.

## Usage

You can either build the jar using gradle (see below) or fetch it from maven central:

    <dependency>
      <groupId>de.tum.in</groupId>
      <artifactId>naturals-util</artifactId>
      <version>0.1.0</version>
    </dependency>

## Building

Build the project using gradle.
All dependencies are downloaded automatically.

    $ ./gradlew build

Or, if you are on windows,

    # gradlew.bat build
