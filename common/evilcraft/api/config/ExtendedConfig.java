package evilcraft.api.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import evilcraft.EvilCraft;
import evilcraft.api.item.ExtendedItemBlockWithMetadata;

/**
 * Registration configurations
 * @author Ruben Taelman
 *
 */
public abstract class ExtendedConfig implements Comparable<ExtendedConfig>{
    
    public int ID;
    public String NAME;
    public String NAMEDID;
    public String COMMENT;
    public Class ELEMENT;
    
    // To store additional stuff inside the config
    public List<ConfigProperty> configProperties = new LinkedList<ConfigProperty>();
    
    /**
     * Create a new config
     * @param defaultId the id for this element (preferably from a config file)
     * @param name the name to be displayed
     * @param namedId a unique name id
     * @param comment a comment that can be added to the config file line
     * @param element the class for the element this config is for
     */
    public ExtendedConfig(int defaultId, String name, String namedId, String comment, Class element) {
        this.ID = defaultId;
        this.NAME = name;
        this.NAMEDID = namedId;
        this.COMMENT = comment;
        this.ELEMENT = element;
        try {
            generateConfigProperties();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Generate the list of ConfigProperties by checking all the fields with the ConfigurableProperty
     * annotation.
     * 
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private void generateConfigProperties() throws IllegalArgumentException, IllegalAccessException {
        for(final Field field : this.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(ConfigurableProperty.class)) {

                ConfigProperty configProperty = new ConfigProperty(
                        field.getAnnotation(ConfigurableProperty.class).category(),
                        this.NAMEDID + "." + field.getName(),
                        field.get(null),
                        field.getAnnotation(ConfigurableProperty.class).comment(),
                        new ConfigPropertyCallback() {
                            @Override
                            public void run(Object newValue) {
                                try {
                                    field.set(null, newValue);
                                } catch (IllegalArgumentException
                                        | IllegalAccessException e) {
                                    // Shouldn't be possible
                                    e.printStackTrace();
                                }
                            }
                        },
                        field.getAnnotation(ConfigurableProperty.class).isCommandable(),
                        field);
                configProperties.add(configProperty);
            }
        }
    }
    
    /**
     * Save this config inside the correct element and inside the implementation if itself
     * @throws Throwable 
     */
    public void save() {
        try {
            // Save inside the self-implementation
            this.getClass().getField("_instance").set(null, this);
            
            // Save inside the unique instance this config refers to (only if such an instance exists!)
            if(this.getHolderType().hasUniqueInstance())
                this.ELEMENT.getMethod("initInstance", ExtendedConfig.class).invoke(null, this);
        } catch (InvocationTargetException e) {
            e.getCause().printStackTrace();
        } catch (IllegalAccessException | IllegalArgumentException
                | NoSuchMethodException
                | SecurityException | NoSuchFieldException e) {
            // Only possible in development mode
            e.printStackTrace();
        }
    }
    
    /**
     * Return the type for which this object holds data
     * @return the elementType of the object to where the config belongs
     */
    public ElementType getHolderType() {
        try {
            return (ElementType) this.ELEMENT.getField("TYPE").get(null);
        } catch (IllegalArgumentException | IllegalAccessException
                | NoSuchFieldException | SecurityException e) {
            // Only possible in development mode
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Will return the instance of the object this config refers to
     * @return instance of sub object
     */
    public Configurable getSubInstance() {
        if(!this.getHolderType().hasUniqueInstance()) return null; // TODO: possibly add a nice exception here
        try {
            return (Configurable) this.ELEMENT.getMethod("getInstance").invoke(null);
        } catch (NoSuchMethodException
                | SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            // Only possible in development mode
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Will return the unique name of the object this config refers to
     * @return unique name of sub object
     */
    public String getSubUniqueName() {
        return NAMEDID;
    }
    
    /**
     * Overridable method that is called after the element of this config is registered.
     */
    public void onRegistered() {
        
    }
    
    @Override
    public int compareTo(ExtendedConfig o) {
        return NAMEDID.compareTo(o.NAMEDID);
    }
    
    /**
     * If the Configurable is registered in the OreDictionary, use this name to identify it.
     * @return the name this Configurable is registered with in the OreDictionary.
     */
    public String getOreDictionaryId() {
        return null;
    }
    
    /**
     * Checks if the eConfig refers to a target that should be enabled.
     * @return if the target should be enabled.
     */
    public boolean isEnabled() {
        return !isForceDisabled() && this.ID > 0;
    }
    
    /**
     * Checks if the eConfig refers to a target that should be force disabled.
     * @return if the target should be force disabled.
     */
    public boolean isForceDisabled() {
        return false;
    }
    
    /**
     * Override this method to prevent configs to be disabled from the config file. (non-zero id's that is)
     * @return if the target can be disabled.
     */
    public boolean isDisableable() {
        return true;
    }
    
    /**
     * Override this method if this block has subtypes.
     * @return if the target block has subtypes.
     */
    public boolean hasSubTypes() {
        return false;
    }
    
    /**
     * If hasSubTypes() returns true this method can be overwritten to define another ItemBlock class
     * @return the ItemBlock class to use for the target block.
     */
    public Class<? extends ItemBlock> getItemBlockClass() {
        return ExtendedItemBlockWithMetadata.class;
    }
    
    /**
     * Call this method in the initInstance method of Configurables if the instance was already set.
     */
    public void showDoubleInitError() {
        EvilCraft.log(this.getClass()+" caused a double registration of "+getItemBlockClass()+". This is an error in the mod code.", Level.SEVERE);
    }
    
    /**
     * A holder class for properties that go inside the config file.
     *
     */
    public class ConfigProperty {
        
        private String category;
        private String name;
        private Object value;
        private String comment;
        private ConfigPropertyCallback callback;
        private boolean isCommandable;
        private Field field;
        
        public ConfigProperty(String category, String name, Object value, String comment, ConfigPropertyCallback callback, boolean isCommandable, Field field) {
            this.category = category;
            this.name = name;
            this.value = value;
            this.comment = comment;
            this.callback = callback;
            this.isCommandable = isCommandable;
            this.field = field;
        }
        
        public ConfigProperty(String category, String name, Object value, ConfigPropertyCallback callback, boolean isCommandable, Field field) {
            this(category, name, value, null, callback, isCommandable, field);
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
            try {
                field.set(null, this.value);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // Won't happen, trust me.
                e.printStackTrace();
            }
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public ConfigPropertyCallback getCallback() {
            return callback;
        }

        public void setCallback(ConfigPropertyCallback callback) {
            this.callback = callback;
        }
        
        public boolean isCommandable() {
            return isCommandable;
        }

        public void setCommandable(boolean isCommandable) {
            this.isCommandable = isCommandable;
        }
        
        public void save(Configuration config) {
            save(config, false);
        }
        
        public void save(Configuration config, boolean forceUpdate) {
            // Sorry, no cleaner solution for this...
            // Reflection could solve it partially, but it'd be still quite ugly...
            String category = getCategory();
            String name = getName();
            Object value = getValue();
            
            Property additionalProperty = null;
            if(value instanceof Integer) {
                additionalProperty = config.get(
                    category,
                    name,
                    (int)value
                    );
                additionalProperty.comment = getComment();
                if(forceUpdate) {
                    getCallback().run((int)value);
                } else {
                    getCallback().run(additionalProperty.getInt());
                }
            } else if(value instanceof Boolean) {
                additionalProperty = config.get(
                    category,
                    name,
                    (boolean)value
                    );
                additionalProperty.comment = getComment();
                if(forceUpdate) {
                    getCallback().run((boolean)value);
                } else {
                    getCallback().run(additionalProperty.getBoolean((boolean)value));
                }
            } else if(value instanceof String) {
                additionalProperty = config.get(
                    category,
                    name,
                    (String)value
                    );
                additionalProperty.comment = getComment();
                if(forceUpdate) {
                    getCallback().run((String)value);
                } else {
                    getCallback().run(additionalProperty.getString());
                }
            } else {
                EvilCraft.log("Invalid config property class.", Level.SEVERE);
            }
        }
    }
    
    public abstract class ConfigPropertyCallback {
        public abstract void run(Object newValue);
    }
}
