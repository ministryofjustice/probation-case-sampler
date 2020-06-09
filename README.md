# Probation Case Sampler
Produces a shortlist of cases for HMI Probation

[![CircleCI](https://circleci.com/gh/ministryofjustice/probation-case-sampler/tree/master.svg?style=svg)](https://circleci.com/gh/ministryofjustice/probation-case-sampler)

### Introduction

The purpose of this service is create a PrimaryCaseSampleProvisional (shortlist) from a PrimaryCaseSample (longlist).

The resultant short list is used by inspectors to determine which cases they will inspect 

### Sampling

A long list is created by getting all the cases in a region that fall in a certain time frame.

Then certain cases are excluded:
 * cases that are not eligible:
    * Sensitive cases (currently determined by having asterisks in the name)
    * Cases that have not yet started
    * Cases that are missing information about gender
 * Cases that are duplicates and not the earliest case in the long list. Matching is determine by:
    * same PNC
    * same CRN
    * Matching first name, last name and date of birth

Once this list has been created, the existing cases are categorised into 1 of 5 stratum:
  * MALE_COMMUNITY_NON_LOW
  * MALE_COMMUNITY_LOW
  * MALE_POST_CUSTODY_NON_LOW
  * MALE_POST_CUSTODY_LOW
  * FEMALE
    
The proportions of these groups are then calculated to ensure fair representation. 
These proportions are used to calculate the number of samples that should be selected from each stratum given a desired total number of samples.
Then a buffer(%) is added to the number of samples in each group (to allow for follow up work where needed)

After the sizes of each Stratum has been determined, the proportion of each cluster, LDU and RO are used to calculate how
many samples should be selected from each sub group (to maintain proportionality of across those grups).

There is a limit here to ensure that no more than 6 cases are selected from any one RO. If this were to happenm cases 
will be picked from other ROs in the same LDU.
(In the rare occurence that there may not be enough cases from ROs within an LDU then there maybe a shortfall where the actual sample size is smaller than the requested size)  

Once the size of each sub group has been calculated, the appropriate cases are randomly selected for each subgroup and aggregated to build the short list. 

Sample sizes will also be adjusted to account for rounding issues. 
If rounding would result in there being few cases within a sub group, then individual cases would be added to each sub group in turn (smallest sub group to largest), until the sample size matches the requested.
Similarly if rounding would result in there being more cases within a subgroup than requested then cases would be removed from subgroups in turn (largest to smallest).   

### Implementation

This service exposes two endpoints:
 * `POST /sample?size=${size of requested sample}` 
 * `POST /analyse?size=${size of requested sample}`

The `/sample` endpoint receives a json list of cases and produces a map of `Stratum` to a list of `Row`s of the cases
that have been selected for the sample. (along with some metadata - Generated ID and timestamp) 

The `/analyse` endpoint also receives a json list of cases and produces the same sample information. Along with that it also produces information about how the allocation of samples across different
Stratum, Clusters, LDUs and ROs was determined. 

Swagger doc available here: https://probation-case-sampler-dev.prison.service.justice.gov.uk/swagger-ui.html#/

#### Testing with the example spreadsheet.

* Download the sample spreadsheet `yr 2 CRC Domain 2 case sample long list v0.1.xlsx` and put it in `/src/test/resources`
* Run `ImportFullSample` test (removing `@Disabled` annotation)
* This will:
    * Create a `sample.json` request file from the spreadsheet
    * Start the app
    * POST the file to `/analyse` endpoint and parse the response
    * Produce a little breakdown of information about the produced sample
    
```
Id:                      d78b278b-238b-4902-bdc0-a61f72e38d65
Timestamp:               2020-06-03T11:59:04.276141
Total sample size:       120
Stratum Summary:
- name: MALE_POST_CUSTODY_NON_LOW      size: 34/55    (original: 28.35%, actual: 28.33%)
- name: MALE_COMMUNITY_NON_LOW         size: 47/75    (original: 38.66%, actual: 39.17%)
- name: FEMALE                         size: 19/32    (original: 16.49%, actual: 15.83%)
- name: MALE_COMMUNITY_LOW             size: 12/19    (original: 9.79%, actual: 10.00%)
- name: MALE_POST_CUSTODY_LOW          size: 8/13     (original: 6.70%, actual: 6.67%)



MALE_POST_CUSTODY_NON_LOW count: 34/55   
- name: Westeros                       size: 16/26    (original: 47.27%, actual: 47.06%)
- name: Utopia                         size: 11/18    (original: 32.73%, actual: 32.35%)
- name: Fantasyland                    size: 7/11     (original: 20.00%, actual: 20.59%)

...
```    


### TODO:

* Move service to probation sub domain once it exists
* Add CRN to case matching rules
* Update list of sentence types and categorisation of sentence types once received


Out of scope for this phase:
* Exclude unpaid work cases where the work requirement is less than 40 hours
* Domain 3 stratification
* Retrieve data from community-api
* Create probation areas register
* Add handling / validation for:
  * Dates match
  * Missing stratification info - e.g. RoSH level, Gender, Sentence Type

