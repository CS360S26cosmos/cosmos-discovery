# cosmos-discovery

A centralized campus event platform that allows students to discover, browse, and RSVP to events while enabling organizers to create, manage, and track attendance efficiently.

# Project Documentation

## Quick Links

- **GitHub Project (Backlog & Kanban Board):**  
  [https://github.com/orgs/CS360S26cosmos/projects/1](https://github.com/orgs/CS360S26cosmos/projects/1)

- **Figma Wireframes Project:**  
[https://www.figma.com/design/C7owJyFcqekHeiwLDJa8yK/cosmos?node-id=1-2](https://www.figma.com/design/C7owJyFcqekHeiwLDJa8yK/cosmos?node-id=1-2)

- **Figma Protype:**
[https://www.figma.com/proto/C7owJyFcqekHeiwLDJa8yK/cosmos?node-id=1-2&t=WuVYljY4x1RCO1Uc-1](https://www.figma.com/proto/C7owJyFcqekHeiwLDJa8yK/cosmos?node-id=1-2&t=WuVYljY4x1RCO1Uc-1)

- **Repository:**
  [https://github.com/CS360S26cosmos/cosmos-discovery](https://github.com/CS360S26cosmos/cosmos-discovery)  

## Table of Contents

- [Team Information](#team-information)
- [Meeting Minutes](#meeting-minutes)
- [UML Diagram](#uml-diagram)
- [CRC Diagrams](#crc-diagrams)
- [Product Backlog](#product-backlog)
- [Wireframes](#wireframes)

## Team Information

- **Team Name:** _cosmos_

| Name            | Roll Number | GitHub ID        |
|---------------|------------|----------------|
| Hafsah Nasir  | 27100237   | HafsahNasir     |
| Hamania Asim  | 27100026   | HamaniaAsim     |
| Ammara Haroon | 27100088   | amm4ra          |
| Elizeh Faisal | 27100052   | ElizehFaisal    |
| Sameen Abid   | 27100337   | sameenabid101   |

## Meeting Minutes

### Meeting 1
Friday, February 20, 2026 – 3:30 PM  

#### Attendance
- Hamania Asim  
- Hafsah Nasir  
- Ammara Haroon  
- Elizeh Faisal  
- Sameen Abid  

#### Key Takeaways
- Created and moved to a **private GitHub organization repository**
- Maintaining documentation inside a **Markdown file instead of Wiki**
- Agreed to use **GitHub Projects** for backlog and Kanban board
- Divided initial responsibilities among team members

### Meeting 2
Monday, 2nd March, 2026 - 3:30 PM

#### Attendance
- Hamania Asim
- Hafsah Nasir
- Ammara Haroon
- Elizeh Faisal
- Sameen Abid

#### Key Takeaways
- Discussed the progress made in Phase 2 of the project.
- TA was informed that we completed the user stories.
- Asked clarification on the roles and responsibilities of club leaders, particularly how their - roles differ from those of organizers.

### Meeting 3
Wednesday, 4th March - 7:30 PM

#### Attendance
- Hamania Asim
- Hafsah Nasir
- Ammara Haroon
- Elizeh Faisal
- Sameen Abid

#### Key Takeaways
- How to login as an admin, how are requests to create an event being approved?
- How to use Figma, making frames and how the site works
- Possible Unique Feature: Points system
- Dividing user stories into frames, discussing workflow

### Meeting 4
Wednesday, 25th March - 10:00 PM

#### Attendance
- Hamania Asim
- Hafsah Nasir
- Ammara Haroon
- Elizeh Faisal
- Sameen Abid

#### Key Takeaways
#### Sprint Planning
- Conducted a sprint planning session with the team  
- Finalized user stories and tasks for the first sprint  
- Began working collaboratively to design and lay out the authorization screens  
- Set up the database to handle login and sign up functionality successfully via Firestore

- For Sprint 1 we decided to break screens into reusable components (e.g., top bar, search bar, navigation bar)  
- Planned to create the main UI structure and began connecting these components across screens so they remain consistent
- Each team member works on a feature in a separate branch, pushes their changes, and submits it for review before it is merged by another team member

### Meeting 5  
Thursday, 26th March – 5:30 PM

#### Attendance  
- Hamania Asim  
- Hafsah Nasir  
- Ammara Haroon  
- Elizeh Faisal  
- Sameen Abid  

#### Key Takeaways  
- Discussed remaining features to implement, including:  
  - Access control integration in Firebase database  
  - Methods for adding and removing event categories  
  - Multi-category selection support in filtering  
  - Personalization through a “suggested” recommendation algorithm  
  - Event cards with interactive details on click  
  - “My Events” functionality for RSVP tracking  

#### Sprint 2 Planning  
- Event details page layout and implementation  
- RSVP functionality that adds events to “My Events”  
- Forgot password and reset password layout and implementation  
- Sidebar navigation to user profile and settings page  
- Friends screen layout and component design

### Meeting 6
Thursday, 29th March – 7:00 PM

#### Attendance  
- Hamania Asim  
- Hafsah Nasir  
- Ammara Haroon  
- Elizeh Faisal  
- Sameen Abid  

#### Key Takeaways
- Reviewed all tasks currently in progress and under review to confirm completion status
- Cross-checked all implemented features against their acceptance criteria
- Identified any missing elements or incomplete requirements
- At this stage for the next phase, the majority of workload is either in progress or in the review stage
- Assigned work for the next sprint with an internal deadline 

Sprint 3 Planning
- Past sprint work is complete, 
- Majority of features are actively being worked on or are pending review
- Assigned the US-08 and US-09, creating new events and editing event details
- Designing tests for the User stories in review
- Assigned US-011 for View and check-in attendee list

### Meeting 7
Thursday, 3rd April – 7:00 PM

### Attendance
Hamania Asim
Hafsah Nasir
Ammara Haroon
Elizeh Faisal
Sameen Abid

### Key Takeaways
Reviewed progress of all user stories across Ready, In Progress, and In Review stages
Confirmed task ownership and current development status for each team member
Ensured features in review are aligned with requirements before final approval
Identified that most remaining work is either actively being developed or under review

### Sprint Progress Overview
US-13 (View Event Analytics) is ready to be picked up – Sameen Abid
US-07 (Rate & Review Attended Event) is in progress – Ammara Haroon
US-22 (Send & Accept Friend Requests) is in progress – Elizeh Faisal
US-21 (Personalized Event Recommendations) is in review – Hafsah Nasir
US-06 (Add Event to Phone Calendar) is in review – Hamania Asim

## UML Diagram
<img width="3381" height="929" alt="nLTjRzis4FwkNq5q7-OiJjSXQ50OGL37LX1XJXp8TejX60f6kiXSPT8bAMTU4M0_OhzmlsH7lTYKpDXE1yo64Ikyvrvul7kEUgyqbJgRfQGlPfHnCg1Ba9wdFZC-fvBEI9HIfRfQItQVQGWYmKViJo1lcf0hmNKk-UcqARe66eEyfD7dH8gCnpsH2ac-kphpBw_UDi6NGYAuXBnxStfwTcP1g9JYyJdX73tG1BPRK1ea2BsS" src="https://github.com/user-attachments/assets/72aed3ca-c89d-4096-8099-f7076712fc69" />


## CRC Diagrams

<!--_Add CRC screenshots below._-->
<img width="5694" height="1625" alt="image" src="https://github.com/user-attachments/assets/2843cedb-e556-4de6-b3d3-f9dd891262e2" />


## Product Backlog

### GitHub Project Link
View full backlog and Kanban board here:  
[https://github.com/orgs/CS360S26cosmos/projects/1](https://github.com/orgs/CS360S26cosmos/projects/1)


### Kanban Board (Screenshot)
<img width="2454" height="1792" alt="08 03 2026 at 02 54 53 PM" src="https://github.com/user-attachments/assets/40ad2948-6698-4ab0-aa5d-f8b5d3cb0467" />
<img width="3024" height="1790" alt="08 03 2026 at 02 58 36 PM" src="https://github.com/user-attachments/assets/1d6791ae-cf2d-49aa-8407-6f5b93595556" />
<img width="1920" height="1798" alt="07 04 2026 at 11 59 30 PM" src="https://github.com/user-attachments/assets/a6469db6-698b-4031-a027-acdc9263e442" />



---

## Wireframes
<img width="3730" height="2453" alt="Student" src="https://github.com/user-attachments/assets/792d46cb-ce79-4d99-87d2-7ddbb3134bac" />
<img width="1107" height="1140" alt="Shared" src="https://github.com/user-attachments/assets/94582b7f-3cf8-4347-8b13-9430e2bb91eb" />
<img width="5694" height="2453" alt="Organiser" src="https://github.com/user-attachments/assets/6cc5bb44-8c5e-44df-9391-05de8f4fb1ed" />
<img width="2549" height="1140" alt="Authentication" src="https://github.com/user-attachments/assets/5f33b09a-5957-4ec1-8d33-ac24f112b5b6" />
<img width="3229" height="2650" alt="Admin" src="https://github.com/user-attachments/assets/fd36a719-bd0b-428c-b975-5c41823b711c" />


#### Figma Link
[https://www.figma.com/design/C7owJyFcqekHeiwLDJa8yK/cosmos?node-id=1-2&t=MyYM3oleXK5TR2gI-1](https://www.figma.com/design/C7owJyFcqekHeiwLDJa8yK/cosmos?node-id=1-2&t=MyYM3oleXK5TR2gI-1)
