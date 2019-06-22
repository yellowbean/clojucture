[![Build Status](https://travis-ci.com/yellowbean/clojucture.svg?branch=master)](https://travis-ci.com/yellowbean/clojucture)

:warning: Pre-alpha - not yet usable! :warning: 

### What & why is clojucture ?

a clojure library for modelling structure products (CLO/MBS/ABS).
it can be used for structuring deals, cashflow projection and investment analysis

The reason behind choosing clojure as modelling language is that :
1. it is simply enough for tech/non-tech background language.
2. Powerful macro language for modelling complex structure of ABS/MBS products.

### Overview
investor side of structured products or structuring an CLO/MBS/ABS deal.
#### Structuring & Issuance
#### Modelling & Analysis
#### Accounting & Surveillance/Reporting 

### Documentation
* https://simplicity.gitbook.io/clojucture/ (working :) ）

### clojucture features
* XML/Protobuf as input/output, language independent ,flexible
* Provides low level functions to build up country specific deal dentures
* High performance with multi-thread and interpolate with Java directly

#### Coverage
##### Asset Type
Asset Type | Support? 
------|----
 Loan  | yes   |
 Mortgage   |  yes  |
 Installments |  yes |
  Commercial Paper |  yes |
 Leasing | yes |


##### Bond Type
Bond Type | Support?
------|----
 Bond with schedule amortization  | yes   |
 Pass through Bond   |  yes  |


##### Analysis
 Assumptions | Support? 
------|----
 Prepayment  | TBD   |
 Default   |  TBD  |
 Recovery  | TBD   |




Credits
----
* [clojure 1.10](https://clojure.github.io/clojure/) 
* [jTablesaw](https://jtablesaw.github.io/tablesaw/)
* [java.time](http://dm3.github.io/clojure.java-time/index.html)

Resource
----
* [learning Clojure](https://practicalli.github.io/clojure/)



Copyright (c) 2018 - 2019 . All rights reserved.
always.zhang@gmail.com 