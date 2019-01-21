[![Build Status](https://travis-ci.org/HBRS-MAAS/ws18-project-commitment_issues.svg?branch=master)](https://travis-ci.org/HBRS-MAAS/ws18-project-commitment_issues)

# MAAS Project - Commitment_Issues

Add a brief description of your project. Make sure to keep this README updated, particularly on how to run your project from the **command line**.

## Team Members
*   Sushant Vijay Chavan - [@Sushant-Chavan](https://github.com/Sushant-Chavan)
*   Ahmed Faisal Abdelrahman - [@AhmedFaisal95](https://github.com/AhmedFaisal95)
*   Abanoub Abdelmalak - [@AbanoubAbdelmalak](https://github.com/AbanoubAbdelmalak)

## Dependencies
* JADE v.4.5.0
* JSON Version 20180813+

## How to run
Just install gradle and run:

    gradle run --args='-packaging -delivery -visualization'

If you get an error saying --args is not supprted, use the below command:

    ./gradlew run --args='-packaging -delivery -visualization'

It will automatically get the dependencies and start JADE with the configured agents.
In case you want to clean you workspace run

    gradle clean

### Distributed Operation
Follow these steps to run multiple stages on different systems.

* Connect all systems to the same network.

* Search for open ports using the command:
```
    netstat -lntu
```
* Look for a port whose state is not LISTEN (this will be the PORT used below).

* Find the host's IP address using the command:
(in field wlp8s0, Inet addr: ...)
```
    ipconfig
```
* Start the host using the command:
```
    gradle run --args='-isHost [host ip address] -localPort [PORT] -[STAGE] -noTK'
```
Example:
```
    gradle run --args='-isHost 192.168.43.113 -localPort 5353 -packaging -noTK'
```

* Start a client using the command:
```
    gradle run --args='-host [HOST IP ADDRESS] -port [HOST LISTENING PORT] -[STAGE]'
```
Example:
```
    gradle run --args='-host 192.168.43.113 -port 5353 -packaging'
```

## Eclipse
To use this project with eclipse run

    gradle eclipse

This command will create the necessary eclipse files.
Afterwards you can import the project folder.
