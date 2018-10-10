
```text
      .:. Smart Properties .:.
       _______________________
      /   //=================/`"-._
     |   ||=================|      D
 jgs  \___\\_________________\__.-"
 ...::::::::::::::::::::::::::::.......
```

# Smart Properties

Do you want to access variable (and potentially sensitive) data while running development tasks in your project?

Try `SmartProperties`.

Take for example this program:

```java
public class Demo {

    public static void main(String[] args) {
        Properties props = SmartProperties.get()
            .property("dev.username", "Your Username")
            .secretProperty("dev.password", "Your Super Secret Password")
            .fromFile(Paths.get("build/dev.properties"));

        System.out.println("Username is " + props.getProperty("dev.username"));
        System.out.println("Password is " + props.getProperty("dev.password"));
    }
}
```

You will be prompted to fill in your data the first time.
Your data will be persisted in Java properties format in the file you choose, and secret data will be obfuscated with Base64 encoding.
It will look something like this:

!["Edit Properties" dialog](docs/images/edit.png)

**`dev.properties`**
```
#Generated with SmartProperties.  Delete or edit this file to reset properties.
#Tue Oct 09 18:40:41 CDT 2018
dev.password=ZG9uJ3RlY2hvcGFzc3dvcmRzZXZlcg\=\=
dev.username=me
```

The output of running `Demo` will be something like this:

```text

> java.exe Demo.class...

      .:. Smart Properties .:.
      _______________________
     /   //=================/`"-._
    |   ||=================|      D
jgs  \___\\_________________\__.-"
...::::::::::::::::::::::::::::.......

Reading properties from file C:\Users\pc\project\build\dev.properties (delete this file to reset)
Missing some properties.  Look for a dialog.
Loaded SmartProperties from file C:\Users\pc\project\build\dev.properties
Username is me
Password is don'techopasswordsever
```

The next time you run the program `SmartProperties` will load your data for you.

```
      .:. Smart Properties .:.
      _______________________
     /   //=================/`"-._
    |   ||=================|      D
jgs  \___\\_________________\__.-"
...::::::::::::::::::::::::::::.......

Reading properties from file C:\Users\pc\IdeaProjects\user-properties-util\build\dev.properties (delete this file to reset)
Loaded SmartProperties from file C:\Users\pc\IdeaProjects\user-properties-util\build\dev.properties
Username is me
Password is don'techopasswordsever
```


Banner based on artwork by [Joan Stark](https://asciiart.website/joan/www.geocities.com/SoHo/7373/indexjava.html).