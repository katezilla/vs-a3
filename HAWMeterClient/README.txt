
# Compilieren:
# ============
#
# HAWMetering starten, dann:
#

cd HAWMeterClient
wsimport -keep -d build -p hawmeterproxy -s src http://localhost:9999/hawmetering/nw?WSDL
javac -d build -cp src src/hawmeterclient/HAWMeterClient.java

# Ausfuehren:
# ===========

cd HAWMeterClient
java -cp build/ hawmeterclient/HAWMeterClient <url> setValue <val>
java -cp build/ hawmeterclient/HAWMeterClient <url> setRange <min> <max>
java -cp build/ hawmeterclient/HAWMeterClient <url> setIntervals <label> <min> <max> <red> <green> <blue> <alpha>
java -cp build/ hawmeterclient/HAWMeterClient <url> clearIntervals
java -cp build/ hawmeterclient/HAWMeterClient <url> setTitle <title>

# <url> kann z. B. sein: http://localhost:9999/hawmetering/nw?WSDL