/* KeyCode_FileBased.java
 * Component: ProperJavaRDP
 * 
 * Revision: $Revision: 1.1.1.1 $
 * Author: $Author: suvarov $
 * Date: $Date: 2007/03/08 00:26:37 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Read and supply keymapping information
 *          from a file
 */
package com.droidcloud.viewer.client.keymapping;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.droidcloud.viewer.client.GreetingService;
import com.droidcloud.viewer.client.GreetingServiceAsync;
import com.droidcloud.viewer.client.Options;
import com.droidcloud.viewer.shared.KeyMapData;
import com.droidcloud.viewer.shared.MapDef;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;



public abstract class KeyCode_FileBased {
	private final GreetingServiceAsync greetingService = GWT
			.create(GreetingService.class);

    private HashMap keysCurrentlyDown = new HashMap<Integer, MapDef>();

    private KeyPressedEvent lastKeyPressedEvent = null;
    

    private boolean lastEventMatched = false;
    

    public static final int SCANCODE_EXTENDED = 0x80;

    public static final int DOWN = 1;

    public static final int UP = 0;

    public static final int QUIETUP = 2;

    public static final int QUIETDOWN = 3;

    private int mapCode = -1;

    private boolean altQuiet = false;

    public boolean useLockingKeyState = true;

    public boolean capsLockDown = false;

    Vector<MapDef> keyMap = new Vector<MapDef>();
    protected Options option;
    

    private void updateCapsLock(KeyPressedEvent e) {

    }
    
 

    /**
     * Constructor for a keymap generated from a specified file, formatted in
     * the manner of a file generated by the writeToFile method
     * 
     * @param keyMapFile
     * @param option
     */
    public KeyCode_FileBased(String keyMapFile, Options option) throws KeyMapException {
        // //logger.info("String called keycode reader");
        int lineNum = 0; // current line number being parsed
        String line = ""; // contents of line being parsed

        boolean mapCodeSet = false;
        readMapFile(keyMapFile);
/*        FileInputStream fstream;
        try {
            fstream = new FileInputStream(keyMapFile);
            readMapFile(fstream);
        } catch (FileNotFoundException e) {
            throw new KeyMapException("KeyMap file not found: " + keyMapFile);
        }
*/        this.option = option;
    }

    void getMap(final String keyMapFile)
    {
    	greetingService.getMap(keyMapFile, new AsyncCallback<KeyMapData>() {
			
			@Override
			public void onSuccess(KeyMapData result) {				
				mapCode = result.getMapCode();
				keyMap = result.getKeyMap();
				Vector<MapDef> newMap = new Vector<MapDef>();
	            
	            Iterator<MapDef> i = keyMap.iterator();
	            while (i.hasNext()) {
	                MapDef current = (MapDef) i.next();
	                if (current.isCharacterDef() && !(current.isAltDown() || current.isCtrlDown() || current.isShiftDown() || current.isCapslockOn())) {
	                    int code = getCodeFromAlphaChar(current.getKeyChar());
	                    if (code > -1) {

	                        newMap.add(new MapDef(code, 0, current.getScancode(),
	                                true, false, false, false));
	                        newMap.add(new MapDef(code, 0, current.getScancode(),
	                                false, false, true, false));
	                    }
	                }
	            }
	            // Commit added mapping definitions
	            keyMap.addAll(newMap);   
			}
			
			@Override
			public void onFailure(Throwable caught) {
				getMap(keyMapFile);
				
			}
		});
    }
    
    /**
     * Read in a keymap definition file and add mappings to internal keymap
     * @param fstream Stream connected to keymap file
     * @throws KeyMapException
     */
    public void readMapFile(String keyMapFile) throws KeyMapException {
        // //logger.info("Stream-based keycode reader");
/*        int lineNum = 0; // current line number being parsed
        String line = ""; // contents of line being parsed

        if (fstream == null)
            throw new KeyMapException("Could not find specified keymap file");

        boolean mapCodeSet = false;

        try {
        	
            DataInputStream in = new DataInputStream(fstream);

    //        if (in == null)
                //logger.warn("in == null");
            
            while (in.available() != 0) {
                lineNum++;
                line = in.readLine();

                char fc = 0x0;
                if ((line != null) && (line.length() > 0))
                    fc = line.charAt(0);

                // ignore blank and commented lines
                if ((line != null) && (line.length() > 0) && (fc != '#') && (fc != 'c')) {
                    keyMap.add(new MapDef(line)); // parse line into a MapDef
                    
                } else if (fc == 'c') {
                    StringTokenizer st = new StringTokenizer(line);
                    String s = st.nextToken();

                    s = st.nextToken();
                    mapCode = Integer.decode(s).intValue();
                    mapCodeSet = true;
                }
            }
*/
    	
            // Add a set of mappings for alphabet characters with ctrl and alt
            // pressed
    		getMap(keyMapFile);
                                       
    }

    /**
     * Given an alphanumeric character, return an AWT keycode
     * @param keyChar Alphanumeric character
     * @return  AWT keycode representing input character, -1 if character not alphanumeric
     */
    private int getCodeFromAlphaChar(char keyChar) {
        if (('a' <= keyChar) && (keyChar <= 'z')) {
            return   keyChar - 'a';
        }
        if (('A' <= keyChar) && (keyChar <= 'Z')) {
            return keyChar - 'A';
        }

        return -1;
    }


    /**
     * Get the RDP code specifying the key map in use
     * @return ID for current key map
     */
    public int getMapCode() {
        return mapCode;
    }

    /**
     * Construct a list of changes to key states in order to correctly send the
     * key action jointly defined by the supplied key event and mapping
     * definition.
     * 
     * @param e
     *            Key event received by Java (defining current state)
     * @param theDef
     */
    public String stateChanges(KeyPressedEvent e, MapDef theDef) {

        String changes = "";

        final int SHIFT = 0;
        final int CTRL = 1;
        final int ALT = 2;
        final int CAPSLOCK = 3;

        int BEFORE = 0;
        int AFTER = 1;

        boolean[][] state = new boolean[4][2];

        state[SHIFT][BEFORE] = e.isShiftKeyDown();
        state[SHIFT][AFTER] = theDef.isShiftDown();

        state[CTRL][BEFORE] = e.isShiftKeyDown() || e.isAltKeyDown();
        state[CTRL][AFTER] = theDef.isCtrlDown();

        state[ALT][BEFORE] = e.isAltKeyDown() || e.isAltKeyDown();
        state[ALT][AFTER] = theDef.isAltDown();

        updateCapsLock(e);

        state[CAPSLOCK][BEFORE] = capsLockDown;
        state[CAPSLOCK][AFTER] = theDef.isCapslockOn();

        if (e.getID() == KeyPressedEvent.KEY_RELEASED) {
        	        	
            AFTER = 0;
            BEFORE = 1;
        }

        if ((e == null) || (theDef == null) || (!theDef.isCharacterDef()))
            return "";

        String up = "" + ((char) UP);
        String down = "" + ((char) DOWN);
        String quietup = up;
        String quietdown = down;

        quietup = "" + ((char) QUIETUP);
        quietdown = "" + ((char) QUIETDOWN);

        if (state[SHIFT][BEFORE] != state[SHIFT][AFTER]) {
            if (state[SHIFT][BEFORE])
                changes += ((char) 0x2a) + up;
            else
                changes += ((char) 0x2a) + down;
        }

        if (state[CTRL][BEFORE] != state[CTRL][AFTER]) {
            if (state[CTRL][BEFORE])
                changes += ((char) 0x1d) + up;
            else
                changes += ((char) 0x1d) + down;
        }

        if (option.isAltkeyQuietEnabled()) {

            if (state[ALT][BEFORE] != state[ALT][AFTER]) {
                if (state[ALT][BEFORE])
                    changes += (char) 0x38 + quietup + ((char) 0x38)
                            + quietdown + ((char) 0x38) + up;
                else {
                    if (e.getID() == KeyPressedEvent.KEY_RELEASED) {
                        altQuiet = true;
                        changes += ((char) 0x38) + quietdown;
                    } else {
                        altQuiet = false;
                        changes += ((char) 0x38) + down;
                    }
                }

            } else if (state[ALT][AFTER] && altQuiet) {
                altQuiet = false;
                changes += (char) 0x38 + quietup + ((char) 0x38) + quietdown
                        + ((char) 0x38) + up + ((char) 0x38) + down;
            }

        } else {
            if (state[ALT][BEFORE] != state[ALT][AFTER]) {
                if (state[ALT][BEFORE])
                    changes += ((char) 0x38) + up;
                else
                    changes += ((char) 0x38) + down;
            }
        }

        if (state[CAPSLOCK][BEFORE] != state[CAPSLOCK][AFTER]) {
            changes += ((char) 0x3a) + down + ((char) 0x3a) + up;
        }

        return changes;
    }

    /**
     * Output key map definitions to a file as a series of single line text
     * descriptions
     * 
     * @param filename
     *            File in which to store definitions
     */
    public void writeToFile(String filename) {
  /*      try {
            FileOutputStream out = new FileOutputStream(filename);
            PrintStream p = new PrintStream(out);

            Iterator i = keyMap.iterator();

            while (i.hasNext()) {
                ((MapDef) i.next()).writeToStream(p);
            }

            p.close();

        } catch (Exception e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }*/
    }

    /**
     * Retrieve the scancode corresponding to the supplied character as defined
     * within this object. Also update the mod array to hold any modifier keys
     * that are required to send alongside it.
     * 
     * @param c
     *            Character to obtain scancode for
     * @return Scancode of supplied key
     */
    public boolean hasScancode(char c) {


        Iterator i = keyMap.iterator();
        MapDef best = null;

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            if (current.appliesTo(c)) {
                best = current;
            }
        }

        return (best != null);

    }

    /**
     * Retrieve the scancode corresponding to the supplied character as defined
     * within this object. Also update the mod array to hold any modifier keys
     * that are required to send alongside it.
     * 
     * @param c
     *            Character to obtain scancode for
     * @param mod
     *            List of modifiers to be updated by method
     * @return Scancode of supplied key
     */
    public int charToScancode(char c, String[] mod) {
        Iterator i = keyMap.iterator();
        int smallestDist = -1;
        MapDef best = null;

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            if (current.appliesTo(c)) {
                best = current;
            }
        }

        if (best != null) {
            if (best.isShiftDown())
                mod[0] = "SHIFT";
            else if (best.isCtrlDown() && best.isAltDown())
                mod[0] = "ALTGR";
            else
                mod[0] = "NONE";
            return best.getScancode();
        } else
            return -1;

    }

    /**
     * Return a mapping definition associated with the supplied key event from
     * within the list stored in this object.
     * 
     * @param e
     *            Key event to retrieve a definition for
     * @return Mapping definition for supplied keypress
     */
    public MapDef getDef(KeyPressedEvent e) {

        if (e.getID() == KeyPressedEvent.KEY_RELEASED) {
            MapDef def = (MapDef) keysCurrentlyDown.get(e
                    .getCharCode());
            registerKeyPressedEvent(e, def);
      //      if (e.getID() == KeyPressedEvent.KEY_RELEASED)
                //logger.debug("Released: " + e.getKeyCode()
           //             + " returned scancode: "
             //           + ((def != null) ? "" + def.getScancode() : "null"));
            return def;
        }

        updateCapsLock(e);

        Iterator i = keyMap.iterator();
        int smallestDist = -1;
        MapDef best = null;

        boolean noScanCode = !hasScancode((char)e.getCharCode());

        while (i.hasNext()) {
            MapDef current = (MapDef) i.next();
            boolean applies;

            if ((e.getID() == KeyPressedEvent.KEY_PRESSED)) {
                applies = current.appliesToPressed(e);
            } else if ((!lastEventMatched) && (e.getID() == KeyPressedEvent.KEY_PRESSED)) {
            	
                applies = current.appliesToTyped(e, capsLockDown, option);
            } else
                applies = false;

            if (applies) {
                int d = current.modifierDistance(e, capsLockDown);
                if ((smallestDist == -1) || (d < smallestDist)) {
                    smallestDist = d;
                    best = current;
                }
            }
        }

      /*  if (e.getID() == KeyPressedEvent.KEY_PRESSED)
            //logger.debug("Pressed: " + e.getKeyCode() + " returned scancode: "
                    + ((best != null) ? "" + best.getScancode() : "null"));
        if (e.getID() == KeyPressedEvent.KEY_TYPED)
            //logger.debug("Typed: " + e.getKeyChar() + " returned scancode: "
                    + ((best != null) ? "" + best.getScancode() : "null"));
*/
        registerKeyPressedEvent(e, best);

        return best;
    }

    /**
     * Return a scancode for the supplied key event, from within the mapping
     * definitions stored in this object.
     * 
     * @param e
     *            Key event for which to determine a scancode
     * @return Scancode for the supplied keypress, according to current mappings
     */
    public int getScancode(KeyPressedEvent e) {
        String[] mod = { "" };

        MapDef d = getDef(e);

        if (d != null) {
            return d.getScancode();
        } else
            return -1;
    }

    private void registerKeyPressedEvent(KeyPressedEvent e, MapDef m) {

        if (e.getID() == KeyPressedEvent.KEY_RELEASED) {
            keysCurrentlyDown.remove(e.getCharCode());
            if ((option.isCapsSendsUpAndDownNotEnabled())
                   ) {
                //logger.debug("Turning CAPSLOCK off - key release");
                capsLockDown = false;
            }
            lastEventMatched = false;
        }

        if (e.getID() == KeyPressedEvent.KEY_PRESSED) {
            lastKeyPressedEvent = e;
            if (m != null)
                lastEventMatched = true;
            else
                lastEventMatched = false;
            if ((option.isCapsSendsUpAndDownEnabled())
                    ) {
                //logger.debug("Toggling CAPSLOCK");
                capsLockDown = !capsLockDown;
            } /*else if (e.getCharCode() == KeyPressedEvent.VK_CAPS_LOCK) {
                //logger.debug("Turning CAPSLOCK on - key press");
                capsLockDown = true;
            }*/            
            
        }

        if (lastKeyPressedEvent != null
                && m != null
                && !keysCurrentlyDown.containsKey(lastKeyPressedEvent
                        .getCharCode())) {
            keysCurrentlyDown.put(lastKeyPressedEvent.getCharCode(), m);
            lastKeyPressedEvent = null;
        }

    }

    /**
     * Construct a list of keystrokes needed to reproduce an AWT key event via RDP
     * @param e Keyboard event to reproduce
     * @return List of character pairs representing scancodes and key actions to send to server
     */
    public String getKeyStrokes(KeyPressedEvent e) {
        String codes = "";
        MapDef d = getDef(e);

        if (d == null)
            return "";

        codes = stateChanges(e, d);

        String type = "";
        
        if (e.getID() == KeyPressedEvent.KEY_RELEASED) {
            if ((option.isCapsSendsUpAndDownNotEnabled())
                    /*&& (e.getCharCode() == KeyPressedEvent.VK_CAPS_LOCK)*/) {
                //logger.debug("Sending CAPSLOCK toggle");
                codes = "" + ((char) 0x3a) + ((char) DOWN) + ((char) 0x3a)
                        + ((char) UP) + codes;
            } else {
                type = "" + ((char) UP);
                codes = ((char) d.getScancode()) + type + codes;
            }
        } else {
            if ((option.isCapsSendsUpAndDownNotEnabled())
                    /*&& (e.getCharCode() == KeyPressedEvent.VK_CAPS_LOCK)*/) {
                //logger.debug("Sending CAPSLOCK toggle");
                codes += "" + ((char) 0x3a) + ((char) DOWN) + ((char) 0x3a)
                        + ((char) UP);
            } else {
                type = "" + ((char) DOWN);
                codes += ((char) d.getScancode()) + type;
            }
        }

        return codes;
    }
}