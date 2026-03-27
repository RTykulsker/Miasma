# MIASMA

## Purpose
The MIASMA (My I Am Safe Messaging App) is a simple web site/service that presents a form to all for the creation of Winlink/RRI/Quick Message email or SMS message. The app writes files that can be imported by PAT for transmission via radio

## Demonstration
- this is only a demonstration.
- this is only a Demonstration.
- this is only a DEMONSTRATION.
- this is only a **DEMONSTRATION.**

## Theory of Operation
- Single Messages are created via the client and submitted via a web interface. This is BYOB (Bring Your Own Device). 
- The Miasma web server writes out a short file with the essentials (FromName, ToAdresses, and MessageText). 
- The Miasma server (non-web) sees that a file has been written to it's **inbox**
- The file is moved (and renamed to prevent collisions) to a **pending** folder
- The file is read by an appropriate **reader** and a list of **transfer** (IASMessage) messages are created
- That list is passed off to an instance of a **Winlink Formatter** (currently only PAT
- The for each address in the addresses field, the formatter creates an appropriate message for delivery, via either email or SMS
- A record of each **accepted** transfer message is written, and the file is moved to the **outbox**

Alternatively, the client can create a spreadsheet file, either Excel (xlsx or xls) or plain CSV.
- one header row
- one message per row
- three columns per message (FromName, ToAddresses, Text) 
- Multiple tabs are supported
- row entries can be blank. If so, I pick up the value from the previous row. I call this feature **auto-ditto** and is useful for sending a bunch of messages from the same FromName, or sending the same message text to a bunch of addresses (although you can also do this via commas, in the ToAddresses field 

Somehow (FTP, sneakernet, whatever) the spreadsheet file gets to the **inbox**. 
I'm adding only one feature for Version 2, the ability to upload the spreadsheet files via the web server.

## Version 2
This is only be a demonstration. I have no desire to support, maintain, update, add features.
<p>
I learned some stuff from version 1. I've stripped out features that I now think are not relevant to a demonstration:
- CommonLog -- there's no need since we'll only run on an internal network
- request sequence number -- no need for tracking
- Supporting both PAT and Winlink Express export -- the idea is to automate. Manually importing into Winlink Express defeats this goal. There are enough breadcrumbs in the code to guide someone who wants to add support for creating files to be importing into Winlink Express
- Supporting multiple SMS providers -- RRI seems to be reliable and without explicit opt-in. There are enought breadcrumbs for someone else to support multiple providers if desired
- lots of flexibility in Excel file format. Let's keep it simple. Every tab with messages needs a header. Tabs can be excluded via config.
- validation of messages -- I think it's important to validate early (BYOD) and late (before sending message). I don't think it's a good idea to also validate when reading a file. There's too much opportunity for one file reader to miss something that another file reader handles.

## Bugs, To Dos, etc
I know that files with the same name can cause conflicts when dropped into the inbox. This is one
reason why I put a leading timestamp on files created via web or upload

