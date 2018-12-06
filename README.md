[![Codacy Badge](https://api.codacy.com/project/badge/Grade/78b10d06c5474a908dcf5ad7da08e269)](https://app.codacy.com/app/molr-developers/molr?utm_source=github.com&utm_medium=referral&utm_content=molr/molr&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.com/molr/molr.svg?branch=master)](https://travis-ci.com/molr/molr)
[![codecov](https://codecov.io/gh/molr/molr/branch/master/graph/badge.svg)](https://codecov.io/gh/molr/molr)
[![GitHub license](https://img.shields.io/github/license/molr/molr.svg)](https://github.com/molr/molr/blob/master/LICENSE)


{% include_relative docs/README.md %}

# molr

A Modular Distributed Execution and Debugging Framework.

As every connoisseur of spy movies knows, a mole is an agent who works for the other side. 
During the development of molr, we decided that we need fresh words ... so our _agents_ are called __moles__ 
(Although we hope they are still good guys ;-).

Furthermore the framework is very modular. This results in the name __molr__:
```
mol[e] + mo[dula]r = molr
``` 

### Getting Started
For the moment, there are no packages released yet. Therefore, the only way to try molr for the moment is to clone 
this repository.

> The current version is heavily under development and not yet stable at all. We hope this to change within a few weeks. 
> Stay tuned! 

### Concept & Vision
The original document about the vision and purpose of molr can be found [here](docs/concepts-and-vision.md). 


As this document reflects the actual state of development, here the current definition of the key entities in molr...

#### Mission
A mission is something that can be run and (in most cases) produces side effects. 

>This is important to note: 
>The main purpose of molr is to produce side effects (e.g. change settings of a device, reconfigure something, ...). 
>Despite the missions can produce outputs, this is not the main focus of molr. In case you are looking for a framework 
>for data processing, molr is definitely not what you need.

This is pretty much the whole definition of a mission. There is no language to which a mission is bound nor any 
interface it has to implement or so. If a mission can finally be used in molr, depends on the fact if there exists a 
_mole_ that can execute (run/debug) a given mission.

#### Mole   
A mole has the responsibility to execute certain type of missions. In principle, a mole is also not bound to be 
implemented in a certain programming language, as the agency understand to interact with remote moles through a well 
defined REST API.
 
However, if a mole is implemented in Java, than it can (in addition to the remote usage) also be used embedded in 
the same jvm as the agency. The responsibilities of a mole are best described by the corresponding java interface:

[Mole.java](https://github.com/molr/molr/blob/master/molr-mole-core/src/main/java/org/molr/mole/core/api/Mole.java)
 
#### Agency
The agency is the central place to manage all available and running missions. The agency keeps track of the existing
moles and the missions that they can execute. Further it delegates requests from clients to the corresponding moles.
Its responsibilities are again best described by a look at its interface:

[molr-agency-core/src/main/java/org/molr/agency/core/Agency.java](molr-agency-core/src/main/java/org/molr/agency/core/Agency.java)

Molr is designed to be completely asynchronous. For this purpose, reactive streams are used. The chosen implementation
for this is [Project Reactor](https://projectreactor.io/), as can be seen from the used classes in the interfaces
(Flux and Mono).

### Package structure

The following is a proposed structure of packages/jars. The main aspect which shall be taken into 
account while splitting packages is that of dependencies. 

| package | description|
|---------| -----------|
|molr-commons | Common elements for the molr project (e.g. domain objects and DTOs). No Spring dependency! |
|molr-agency-core | Contains all the classes required in molr agency related packages (e.g. interfaces).|
|molr-agency-remote | The remote implementation of a molr agency. It uses spring webflux to connect to an agency server|
|molr-agency-server | Provides a REST service representing an agency. This package has Java server dependency (e.g. Tomcat). |
|molr-mole-core | Contains the interfaces and default implementations for moles, as well as utility methods.|
|molr-mole-remote | Contains the implementation of a remote mole, which can delegate to any mole reachable through a Web API. Depends on Spring webflux. |
|molr-mole-server | Exposes any mole as a REST service. This package has Java server dependency (e.g. Tomcat). |
|molr-testing | Contains utility classes for testing molr services. |
