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

### clojucture features
* Json as input/output, language independent ,flexible
* Provides low level functions to build up country specific deal dentures
* High performance with multi-thread and interpolate with JVM world
* Heavily using `core.match` to manage complexity of structured finance world

#### Coverage

##### Deal Type
Deal Type | Support ? 
 ---|---
China Interbank deal | yes |
 .... | WIP |

##### Asset Type
Asset Type | Support?  | Stress Support ? 
------|----|-----
 Loan  | yes   | WIP |
 Mortgage   |  yes  | WIP |
 Installments |  yes |WIP |
 Commercial Paper |  yes |WIP |
 Leasing | yes | WIP |


##### Bond Type
Bond Type | Support?
------|----
 Bond with schedule amortization  | yes   |
 Pass through Bond   |  yes  |


Development Internal
----
The code base organized in a simple way

* clojucture.io  
    I/O functions that will output analysis/cashflow result in form of CSV/HTML/JSON
* clojucture.account  
    includes different account types
* clojucture.asset  
    includes different asset types. These assets will be in `pool` and can be projected cashflow from.
* clojucture.assumption  
    includes assumption related functions to create assumptions. These assumptions will applied in `pool` level to project cashflows in a stressed way.
* clojucture.bond  
    includes different types of bonds will be structured in a deal.
* clojucture.core  
    nothing here 
* clojucture.expense  
    includes different types of expenses will be structured in a deal.
* clojucture.pool  
    `pool` related functions.
* clojucture.server  
    an REST server interface that can read deal / performan deal analysis.
* clojucture.spv  
    an abstraction of SPV/deal, which holds one or more `pool` and `bonds` and a `waterfall`/distribution rules.
* clojucture.trigger  
    includes trigger related function, the trigger will affect the sequence of `waterfall` being used  in distributing funds to bonds/expenses.
* clojucture.util  
    utility functions to calculate cashflow, generate dates vectors etc.
* clojucture.util_cashflow  
    new
* clojucture.waterfall  
    TBD

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