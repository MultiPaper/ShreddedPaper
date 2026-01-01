package io.multipaper.shreddedpaper.config;

import io.papermc.paper.configuration.ConfigurationLoaders;
import io.papermc.paper.configuration.ConfigurationPart;
import io.papermc.paper.configuration.constraint.Constraint;
import io.papermc.paper.configuration.constraint.Constraints;
import io.papermc.paper.configuration.mapping.InnerClassFieldDiscoverer;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.leangen.geantyref.GenericTypeReflector.erase;

public class ShreddedPaperConfigurationLoader {

    public static void init(File file) {
        try {
            YamlConfigurationLoader.Builder loaderBuilder = ConfigurationLoaders.naturallySorted();
            loaderBuilder.defaultOptions(options -> options.header(ShreddedPaperConfiguration.HEADER));

            Path configFile = file.toPath();
            YamlConfigurationLoader loader = loaderBuilder
                    .defaultOptions(applyObjectMapperFactory(createObjectMapper().build()))
                    .path(configFile)
                    .build();
            ConfigurationNode node;
            if (Files.exists(configFile)) {
                node = loader.load();
            } else {
                node = CommentedConfigurationNode.root(loader.defaultOptions());
            }

            String before = node.toString();
            setFromProperties(node);

            ShreddedPaperConfiguration instance = node.require(ShreddedPaperConfiguration.class);
            transformLegacyConfig(node, instance);

            for (Object key : node.childrenMap().keySet()) {
                node.removeChild(key);
            }

            node.set(instance);

            if (!node.toString().equals(before)) {
                loader.save(node);
            }

            ShreddedPaperConfiguration.set(instance);
        } catch (ConfigurateException e) {
            throw new RuntimeException("Could not load shreddedpaper.yml", e);
        }
    }

    private static void setFromProperties(ConfigurationNode node) {
        for (Map.Entry<Object, Object> property : System.getProperties().entrySet()) {
            if (property.getKey().toString().startsWith("shreddedpaper.")) {
                String key = property.getKey().toString().substring("shreddedpaper.".length());
                try {
                    node.node((Object[]) key.split("\\.")).set(property.getValue());
                } catch (SerializationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static ObjectMapper.Factory.Builder createObjectMapper() {
        return ObjectMapper.factoryBuilder()
                .addConstraint(Constraint.class, new Constraint.Factory())
                .addConstraint(Constraints.Min.class, Number.class, new Constraints.Min.Factory())
                .addDiscoverer(InnerClassFieldDiscoverer.globalConfig());
    }

    private static UnaryOperator<ConfigurationOptions> applyObjectMapperFactory(final ObjectMapper.Factory factory) {
        return options -> options.serializers(builder -> builder
                .register(type -> ConfigurationPart.class.isAssignableFrom(erase(type)), factory.asTypeSerializer())
                .registerAnnotatedObjects(factory));
    }

    private static void transformLegacyConfig(ConfigurationNode node, ShreddedPaperConfiguration config) {
        // getAndRemove(node, "oldValue.oldie", value -> config.newSection.newValue.newie = value.getString());
    }

    private static void getAndRemove(ConfigurationNode node, String key, ExceptionableConsumer<ConfigurationNode> consumer) {
        if (System.getProperty(key) != null) {
            try {
                BasicConfigurationNode systemNode = BasicConfigurationNode.root();
                systemNode.set(System.getProperty(key));
                consumer.accept(systemNode);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String[] parts = key.split("\\.");

        for (int i = 0; i < parts.length; i++) {
            if (node.isMap() && node.childrenMap().containsKey(parts[i])) {
                if (i == parts.length - 1) {
                    try {
                        consumer.accept(node.childrenMap().get(parts[i]));
                        node.removeChild(parts[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    node = node.childrenMap().get(parts[i]);
                }
            } else {
                return;
            }
        }
    }

}
