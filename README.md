# hubitat-litetouch-2000

#Instructions for LiteTouch 2000 plugin for Hubitat

## = Introduction =
This plugin is for the Hubitat Elevation and adds support for the LiteTouch 2000 system.  It supports dimmer and relay loads, and polling with the right cable.  It will NOT work with the 5000LC controller, it only supports the ASCII protocol which is spoken by both the Compact and Standard CCU.  The ASCII protocol on the 5000LC is completely different.

The protocol on the Standard and Compact CCU doesn't support anything to enumerate the loads, so you'll need to define them all yourself when setting up the plugin.  Examples of loads would be 01-1, 03-4, or 10-6.  These are NOT keypad identifiers, these are load identifiers.  If you have the standard CCU, you can go into the LCD interface on it and view the loads that are assigned to each button.  Or, you can open up your panels and look at the dimmer and relay modules to see where the little dials are set to find out the ID.  The "10" in "10-6" is the module number, and the "6" is the output number.  10-6 would be the load.  Keep track of which are dimmer modules and which are relay modules.

If you plan on going into the LCD interface to find out loads, it's a really slow process going through the menus.  You'll need to know the button ID's to find the load they are assigned to.  To do that, pop the cover off the switch.  Usually the installer has written an identifier on the keypad like 1a or 2c.  Buttons are numbered from 1-9.  So a button ID might be 1a-1 or 2c-6.  If you go into your LCD menu, you can use the view/change button menu to see which loads are assigned to that button.  Note that some buttons will be assigned to scenes, and it's not a 1 to 1 mapping.  The easiest way is to open up your panel, look at the modules to find out the ID and how many outputs it has, and then just keep a list.  So if you had a module that was set to 05 on the two dials and it had 6 outputs, you'd have 05-1, 05-2, 05-3, 05-4, 05-5, and 05-6.  After you get them all in there, you'll need to walk through your house with turning on/off the different loads in Hubitat to figure out what they are for.  

Or maybe you are lucky and the installer left you a spreadsheet with all of this info.  :)

## = Requirements =
  * Hubitat
  * Standard or Compact CCU (NOT an LT5000 controller, it uses a different protocol)
  * A working IP->serial converter.  You can buy one, or you could build one with a Raspberry Pi, a usb->serial dongle, and the "socat" command.  9600 8N1 (e.g. "socat -d -d TCP-LISTEN:2001,fork,reuseaddr /dev/ttyUSB0,raw,echo=0,b9600,cs8,parenb=0,clocal=1")
  * LiteTouch integration cable.  You can make this with pins 2, 3, and 5 straight through.  If you want polling to work, you will need to bridge pins 7 & 8 at the CCU side (don't hook them up to your serial converter).  The best way to make a cable is to get 2 RJ45->RS232 converters (ask your favorite network guy, he's got a drawer full)

## = Setup = 
  * Install the driver, copy the code or the link to the code into the Drivers Code section of your Hubitat
  * Install the app, copy the code or link into the Apps Code section of your Hubitat
  * Create a new user app and find the LiteTouch 2000 App.
  * Fill out the IP, port, and polling interval
  * Create your CSV like the sample 
  * Click save

Now all of your devices will show up.  As they are polled, the statuses will start updating.  Setting a load through Hubitat will poll the device immediately.  Otherwise, polling runs down a list of loads and polls one every two seconds, only to start back at the top after they've all been polled.  There are some limitations in the serial protocol which forced it to be done this way.  This means that if you have 48 total loads and you change one with a wall keypad, it could take up to 96 seconds for the Hubitat to pick it up.  

You can now name your devices and start using them in Rules, Scenes, and Dashboards.

# = Making the cable = 
The LiteTouch system has TWO different serial cables.  There's a programming cable, and an integration cable.  The programming cable is used for LiteWare.  It has pins 2, 3, and 5 straight though, and pins 7&8 are crossed end to end.  The implementation cable is the same, except pins 7&8 are not connected to the serial converter side at all, but are bridged together on the CCU side.

The easiest way to do this is to get a couple of RJ45->RS232 adapters.  Cisco and Juniper ship them with every piece of network equipment, so if you know an IT guy, he can get them for you.  Or, you can order them off amazon.  Connect them together with a standard ethernet cable (not a crossover).  Then, pop the plug out of one end, cut the wires for pins 7&8 and twist them together and tape.  Plug the modified end into the CCU.

You should also note that the plug on the CCU side is a female plug.  So pick up a male->male gender bender while you're at it, or make sure one of the RJ45->RS232 things has male pins on it.

## = Known issues = 
  * Polling is broken.  Data coming back from the polling is not being read, and the polling loop doesn't seem to be working.
