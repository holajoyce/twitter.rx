# Tweets.Rx

<!--
<p class="image" >
<img src="http://oak.is/assets/animated-svg-ff.svg" border="0" /></p>
<img src="images/twitter-512px.svg" height="10%" width="10%" border="0" /></p>
-->

<img src="images/fillon.jpg"  style="border-width: 0px" />

#### Twitter Pharmaceuticals Ad Network

---

### User Story

As an ad network for pharmaceuticals on Twitter, I would like to be able to display my ads (along with a bid) to relevant users on Twitter. 

<img src="images/ads.png"  style="border-width: 0px"  width="80%" height="80%"/>

---

In this project, relevant user is defined based on the content of the users' tweet


---

#### Backloged features

- run batch jobs to find common symptoms that occur together (per condition)

<img src="images/hortonworks.jpg"  style="border-width: 0px" />

---

### Data Model

<img src="images/drug_campaigns.png"  style="border-width: 0px" />

---

#### [Current Stack](https://cloudcraft.co/view/54cdb7bf-5c7b-440f-9f5a-4878b34aba78?key=3m40jn0enpfd2t90)

<img src="images/streaming_arch.png" style="border-width: 0px"  />

---

## The jury is still out

- spark streaming vs samza

---

### Why Spark Streaming

- joins two batches that are in the same time interval

---

### Why Samza

- Table-table join
- Stream-table join
- Stream-stream join

---

Spark Streaming’s updateStateByKey approach to store mismatch events cannot handle large number of events, which causes the inefficience in Spark Streaming.

---

#### Samza Stream-table Join

Common in advertising, relevance ranking, fraud detection and other domains. 

Activity events such as page views generally only include a small number of attributes, such as the ID of the viewer and the viewed items, but not detailed attributes of the viewer and the viewed items, etc.

---

#### Frontend

<img src="images/frontend.png"  style="border-width: 0px" />

---

## About Me

<img alt="" class="avatar" height="230" src="https://avatars1.githubusercontent.com/u/5039790?v=3&amp;s=460" width="230"  style="border-width: 0px">

### Joyce Chan


B.Sc Computer Science and Math

Software engineer in social media & healthcare analytics, Sysomos, Marketing optimization startup.


