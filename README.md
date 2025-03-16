<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Unlicense License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/arcode13/dormhub-web">
    <img src="images/logo.png" alt="Logo" width="80" height="80">
  </a>

  <h3 align="center">Student Dormitory Management Application</h3>

  <h3 align="center">“DormHub”</h3>

  <p align="center">
    <a href="https://dormhub.com"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://dormhub.id">View Demo</a>
    &middot;
    <a href="https://github.com/arcode13/dormhub-web/issues/new?labels=bug&template=bug-report---.md">Report Bug</a>
    &middot;
    <a href="https://github.com/arcode13/dormhub-web/issues/new?labels=enhancement&template=feature-request---.md">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

[![DormHub Screenshot][product-screenshot]](https://dormhub.id)

Telkom University dormitories play a crucial role in supporting students' academic and social life during their early semesters. However, managing the dormitories comes with several challenges, such as:  
✅ Unorganized room information records  
✅ Manual reporting process for lost items and permission requests  
✅ Difficulty in handling student complaints efficiently  

### **Why DormHub?**
With a large number of students living in dormitories, an efficient management system is essential to ensure smooth operations. The existing system relies heavily on conventional methods like manual record-keeping or outdated applications, leading to:  
❌ Delays in service responses  
❌ Administrative errors  
❌ Lack of transparency in dormitory management  

### **DormHub as a Solution**
DormHub is a digital platform designed to streamline dormitory management by integrating essential features into a centralized system:  
🔹 **Room Information Management** – Easily track room availability and assignments  
🔹 **Item & Permission Reporting** – A fast and well-documented process for reporting lost or damaged items  
🔹 **Helpdesk & Complaints Handling** – Ensures student complaints are addressed quickly and efficiently  
🔹 **Centralized Admin Control** – Provides dormitory administrators with full control over user data and services  

With DormHub, students at Telkom University can enjoy a more comfortable, efficient, and transparent dormitory experience. 🚀  

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

This section should list any major frameworks/libraries used to bootstrap your project. Leave any add-ons/plugins for the acknowledgements section. Here are a few examples.

* [![Java][Java.com]][Java-url]
* [![Spring Boot][SpringBoot.com]][SpringBoot-url]
* [![Thymeleaf][Thymeleaf.com]][Thymeleaf-url]
* [![Bootstrap][Bootstrap.com]][Bootstrap-url]
* [![Flutter][Flutter.dev]][Flutter-url]
* [![MySQL][MySQL.com]][MySQL-url]
* [![JQuery][JQuery.com]][JQuery-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

This section provides instructions on how to set up and run the project locally.  
Follow these steps to get started.

### Prerequisites

Ensure you have the following installed before proceeding:

1. **Java Development Kit (JDK)**  
   - Download and install **JDK 17 or newer** from:  
     [Adoptium Temurin JDK](https://adoptium.net/)  
   - Verify installation:  
     ```sh
     java -version
     ```

2. **Apache Maven** (if not installed)  
   - Download Maven from:  
     [Apache Maven Official Site](https://maven.apache.org/download.cgi)  
   - Extract the archive and add the `bin` directory to your system `PATH`.  
   - Verify installation:  
     ```sh
     mvn -v
     ```

3. **MySQL Database**  
   - Install [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)  
   - Start MySQL service and create a database named `dormhub_db`.

---

### Installation

_Follow the steps below to install and set up the application properly._

1. **Clone the repository** 
   ```sh
   git clone https://github.com/arcode13/dormhub-web.git
   cd dormhub-web

2. **Install dependencies using Maven**
   ```sh
   mvn clean install

3. **Configure database connection in**
   ```sh
   spring.datasource.url=jdbc:mysql://localhost:3306/dormhub_db
   spring.datasource.username=root
   spring.datasource.password=

4. **Run the application**
   ```sh
   mvn spring-boot:run

5. **Access the application at**
   ```sh
   http://localhost:8080
   
<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

-

_For more examples, please refer to the [Documentation](https://example.com)_

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->
## Roadmap

- [x] Add Chatbot AI
- [ ] Multi-language Support
    - [ ] Indonesian
    - [ ] English

See the [open issues](https://github.com/arcode13/dormhub-web/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Top contributors:

<a href="https://github.com/arcode13/dormhub-web/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=arcode13/dormhub-web" alt="contrib.rocks image" />
</a>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->
## License

Distributed under the Unlicense License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

DormHub.id - [@dormhub.id](https://instagram.com/dormhub.id) - support@dormhub.id

Project Link: [https://github.com/arcode13/dormhub-web](https://github.com/arcode13/dormhub-web)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

Use this space to list resources you find helpful and would like to give credit to. I've included a few of my favorites to kick things off!

* [Choose an Open Source License](https://choosealicense.com)
* [GitHub Emoji Cheat Sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet)
* [Malven's Flexbox Cheatsheet](https://flexbox.malven.co/)
* [Malven's Grid Cheatsheet](https://grid.malven.co/)
* [Img Shields](https://shields.io)
* [GitHub Pages](https://pages.github.com)
* [Font Awesome](https://fontawesome.com)
* [React Icons](https://react-icons.github.io/react-icons/search)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/arcode13/dormhub-web.svg?style=for-the-badge
[contributors-url]: https://github.com/arcode13/dormhub-web/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/arcode13/dormhub-web.svg?style=for-the-badge
[forks-url]: https://github.com/arcode13/dormhub-web/network/members
[stars-shield]: https://img.shields.io/github/stars/arcode13/dormhub-web.svg?style=for-the-badge
[stars-url]: https://github.com/arcode13/dormhub-web/stargazers
[issues-shield]: https://img.shields.io/github/issues/arcode13/dormhub-web.svg?style=for-the-badge
[issues-url]: https://github.com/arcode13/dormhub-web/issues
[license-shield]: https://img.shields.io/github/license/arcode13/dormhub-web.svg?style=for-the-badge
[license-url]: https://github.com/arcode13/dormhub-web/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/othneildrew
[product-screenshot]: images/screenshot.png
[Java.com]: https://img.shields.io/badge/Java-000000?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://java.com/
[SpringBoot.com]: https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[SpringBoot-url]: https://spring.io/projects/spring-boot
[Thymeleaf.com]: https://img.shields.io/badge/Thymeleaf-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white
[Thymeleaf-url]: https://www.thymeleaf.org/
[Bootstrap.com]: https://img.shields.io/badge/Bootstrap-563D7C?style=for-the-badge&logo=bootstrap&logoColor=white
[Bootstrap-url]: https://getbootstrap.com/
[Flutter.dev]: https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white
[Flutter-url]: https://flutter.dev/
[MySQL.com]: https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white
[MySQL-url]: https://www.mysql.com/
[JQuery.com]: https://img.shields.io/badge/jQuery-0769AD?style=for-the-badge&logo=jquery&logoColor=white
[JQuery-url]: https://jquery.com/
