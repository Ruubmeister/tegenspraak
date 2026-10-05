package nl.rubium.tegenspraak.desktop.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An application on a virtual desktop. The "type" property selects the implementation, both in the case files and in
 * the JSON sent to the frontend. Adding a new kind of app means adding a record here and a matching frontend component.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MailApp.class, name = "mail"),
    @JsonSubTypes.Type(value = FilesApp.class, name = "files")
})
public sealed interface DesktopApp permits MailApp, FilesApp {

    String id();

    String title();
}
