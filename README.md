# Terasology Applet

Run Terasology as an applet in your favorite browser. When it works, anyway :-)

The Java applet technology has aged a fair bit and can be tricky to keep functional, especially with tighter security regulations for Java in a browser.

This repository is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

## Technical

The applet is signed with a code certificate provided for free by [Certum](http://www.certum.eu) - thanks for the free Open Source Code Signing certificate!

Applets need extra security privileges so there should be a prompt in the browser showing our certificate details, registered by `Rasmus 'Cervator' Praestholm`.
 
You can enable the Java console and debug logging for applets via Java settings, which on Windows is accessed through the control panel.

Log files may end up at a path like: `C:\Users\[user]\AppData\LocalLow\Sun\Java\Deployment\log` or equivalent on other OSes.

Local temp files may be found somewhere like: `C:\Users\[user]\AppData\Local\Temp\lwjglcache` as we're using the LWJGL applet setup at present.

## How to create a code signing certificate

Required resources:

* A Certum root certificate
* openssl (http://www.openssl.org)
* jarsigner (comes with Java JDK)
 
Required files:

* CodeSigningCertFromCertum.cer (the certificate)
* CodeSigningCertFromCertum.p12 (the private key)
* bundle.pem (the certificate chain)

Convert code signing CER file into plain-text .pem format 

    openssl x509 -inform DES -in CodeSigningCertFromCertum.cer -out CodeSigningCertFromCertum.pem

Then extract private key from p12 file:

    openssl pkcs12 -in CodeSigningCertFromCertum.p12 -out private.key 

Then create a new keystore based on the private key, the certificate and the cerfificate chain to the root CA

    openssl pkcs12 -export -inkey private.key -in CodeSigningCertFromCertum.pem -certfile bundle.pem -out new_certificate.pfx

Convert pkcs12 keystore into a java keystore

    keytool -v -importkeystore -srckeystore new_certificate.pfx -srcstoretype PKCS12 -destkeystore certum.jks -deststoretype JKS

Verify the content (one private key named '1' with certficate chain length of 3)

    keytool -v -list -keystore certum.jks

Rename the alias of the the entry to something more useful

    keytool -changealias -destalias cervator -keystore certum.jks -alias 1 

Finally, we can use the keystore and a time-stamping authoritiy to sign the jar

    jarsigner -tsa http://time.certum.pl/ -keystore certum.jks applet.jar cervator
    
## Related Links

* [Signed Applets by Oracle](http://java.sun.com/developer/onlineTraining/Programming/JDCBook/signed.html)
* [LWJGL applet loader](http://lwjgl.org/wiki/index.php?title=Deploying_with_the_LWJGL_Applet_Loader_-_Introduction)
* [Tutorial page specific to Groovy in an applet](http://www73.pair.com/bgw/applets/GroovyDemo/)
  * Includes instructions for minimizing the size of the Groovy jar
* [how to enable the Java Console for applet output if need be](http://www.java.com/en/download/help/javaconsole.xml)
       