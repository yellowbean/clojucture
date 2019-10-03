[![Build Status](https://travis-ci.com/yellowbean/clojucture.svg?branch=master)](https://travis-ci.com/yellowbean/clojucture)

:warning: Pre-alpha - not yet usable! :warning: 

### Clojucture
a library that provides buidling blocks for:
* modelling structure products (CLO/MBS/ABS).
* structuring deals, cashflow projection and investment analysis
* monitoring and ongoing operation support (accounting, issuance reports etc)


### Overview
### Clojucture features
* Json as input/output, language independent ,flexible
* Provides low level functions to build up country specific deal dentures
* High performance with multi-thread and interpolate with JVM world
* Heavily using `core.match` to manage complexity of structured finance world
* Shipped with a REST server for centerilized operation
* Visualized waterfall presentation with [graphviz](https://www.graphviz.org/)
### Building blocks for structure finance world
* Structuring
* Investment analysis
* Issuance solution

Development Internal
----

Namespaces
-----
The code base organized in a simple way

* clojucture.io  
    I/O functions that will output analysis/cashflow result in form of CSV/HTML/JSON
* clojucture.account  
    includes different `account` types. Accounts used to be collect cashes and distribute cash to liability side of the `deal`
* clojucture.asset  
    includes different `asset` types. These assets will be in `pool` and can be projected cashflow from.
* clojucture.assumption  
    includes assumption related functions to create assumptions. These assumptions will applied in `pool` level to project cashflows in a stressed way.
* clojucture.bond  
    includes different types of `bonds` will be structured in a deal.
* clojucture.core  
    nothing here 
* clojucture.expense  
    includes different types of `expenses` will be structured in a deal.
* clojucture.pool  
    `pool` related functions.
* clojucture.server  
    an REST server interface that can read deal / perform deal analysis.
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

Tutorial
-----
Structured finance is most complex financial instrument in fix-income business. We will start with a redicoulsy unreal example to start with .

Components of a deal
------
A deal usually can be break into 3 parts:
* pool, a `pool` will contains `assets` which generate cashflow in the future.
* waterfall/rules, that describe how cashflow generated from pool is being aggregated and distributed to `expenses` and `bonds`.
* bonds, a set of notes that has a balance ,interest rate but different priorities when got paid.

First deal
------



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