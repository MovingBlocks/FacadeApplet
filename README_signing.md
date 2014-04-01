
## How to create a code signing certificate

Required software:

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