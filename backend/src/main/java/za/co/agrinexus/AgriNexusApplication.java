/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
 */
package za.co.agrinexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude={UserDetailsServiceAutoConfiguration.class})
public class AgriNexusApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgriNexusApplication.class, (String[])args);
    }
}
