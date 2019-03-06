[![Build Status](https://travis-ci.com/yellowbean/clojucture.svg?branch=master)](https://travis-ci.com/yellowbean/clojucture)

### What & why is clojucture ?

a clojure library for modelling structure products (CLO/MBS/ABS/CMBS).
it can be used for cashflow projection and investment analysis.

The reason behind choosing clojure as modelling language is that :
1. It is simply enough for tech/non-tech background language.
2. Powerful macro language for modelling complex structure of ABS/MBS products.


### Overview
investor side of structured products or structuring an CLO/MBS/ABS deal.


### Documentation
* https://simplicity.gitbook.io/clojucture/

### clojucture features
* Takes XML based input, language independent ,flexible
* Provides low level functions to build up country specific deal dentures

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



Copyright (c) 2019 . All rights reserved.
always.zhang@gmail.com 