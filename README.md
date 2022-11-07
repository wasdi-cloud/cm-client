# cm-client

This is a Java/SpringBoot application that creates the (Copernicus Marine) configuration-content for WASDI appconfig.json.

Before running the application, make sure to add valid credentials (obtained from the Copernicus Marine site) to the application.properties file.

Eventually, if new product_types/datasets should be added to WASDI, make sure you add the new product_type value to the "cm.accepted.product.types" entry of the application.properties file.


- To build the application, open its root folder using a command-line tool: 
e.g., C:\work\dev\cm-client

- Build the jar:
mvn clean install -DskipTests

- Run the application:
java -jar .\target\cm-client-0.0.1-SNAPSHOT.jar

- Check the resulting cm_appconfig.json file.
C:\work\dev\cm-client\cm_appconfig.json

- Opwn the file with Notepad++ and format it properly:
Notepadd++ > Plugins > JSON Viewer > Format JSON

- Copy the content and paste it on the appconfig.json, replacing what is the current configuration for CM:

        {
            "name": "CM",
            "indexname": "platformname",
            "indexvalue": "CM",
            "filters": [
                {
                    "indexname": "producttype",

...

                    "indexvalue": "5727.9169921875"
                }
            ]
        }
