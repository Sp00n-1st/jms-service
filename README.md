# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

- [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
- [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.0/maven-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/3.4.0/maven-plugin/build-image.html)

### Penjelasan Flow Aplikasi

- Get data message dari ActiveMQ Artemis
- Ketika setelah get data message di setiap kelipatan 5000 akan dilakukan insert batch ke dalam database, untuk kelipatan batch nya bisa di setting di application.properties pada properties batch-size-insert
- Ketika data message berhasil di insert ke database maka akan dilakukan manual acknowledge, dan jika terjadi error pada saat insert ke database maka proses manual acknowledge tidak akan dilakukan sehingga message tidak akan diconsume
- Untuk field dan table apa saja yang diperlukan bisa di check di file query.sql
- Tambahan fitur untuk check message dari queue untuk setting waktu schedule bisa di check pada application.properties

### HOW TO RUN

Running For Get Message From Queue And Save To Database

- java.exe -Xms2g -Xmx8g -XX:+UseG1GC -jar .\jmscore-0.0.1-SNAPSHOT.jar

Running For Check Message From Queue With Schedule

- java.exe -Xms2g -Xmx8g -XX:+UseG1GC -jar .\jmscore-0.0.1-SNAPSHOT.jar true true

Running For Check Message From Queue Without Schedule

- java.exe -Xms2g -Xmx8g -XX:+UseG1GC -jar .\jmscore-0.0.1-SNAPSHOT.jar true false
