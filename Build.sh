#!/bin/bash
gradlew build -x lint -x lintVitalRelease -x test
gradlew assembleDebug
gradlew :app:buildDebug
