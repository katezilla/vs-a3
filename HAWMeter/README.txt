
Compilieren:
============

cd HAWMeter
javac -d build -cp src:lib/jcommon-1.0.16.jar:lib/jfreechart-1.0.13.jar src/hawmetering/HAWMetering.java

Ausfuehren:
===========

cd HAWMeter
java  -cp build:images:lib/jcommon-1.0.16.jar:lib/jfreechart-1.0.13.jar hawmetering.HAWMetering
