package edu.northeastern.cs5500.starterbot.seeder;
/** This class contains predefined interests */
public final class InterestConstants {
    private InterestConstants() {
        // restrict instantiation
    }

    public static final String[] COURSE_PREREQUISITE = {
        "CS5001:Intensive Foundations of Computer Science",
        "CS5002:Discrete Structures",
        "CS5004:Object-Oriented Design",
        "CS5008:Data Structures, Algorithms, and Their Applications within Computer Systems"
    };

    public static final String[] COURSE_CORE = {
        "CS5800:Algorithms", "CS5010:Programming Design Paradigm"
    };

    public static final String[] COURSE_SYSTEM_SOFTWARE = {
        "CS5400:Principles of Programming Languages",
        "CS5500:Foundations of Software Engineering",
        "CS5520:Mobile Application Development",
        "CS5600:Computer Systems",
        "CS5610:Web Development",
        "CS5700:Fundamentals of Computer Networking",
        "CS6410:Compilers",
        "CS6510:Advanced Software Development",
        "CS6650:Building Scalable Distributed Systems",
        "CS6710:Wireless Network"
    };

    public static final String[] COURSE_THEORY_SECURITY = {
        "CS6760:Privacy, Security, and Usability",
        "CS7805:Theory of Computation",
        "CS7810:Foundations of Cryptography",
        "CY5770:Software Vulnerabilities and Security",
        "CY6740:Network Security",
        "CY6750:Cryptography and Communications Security"
    };

    public static final String[] COURSE_AI_DATA_SCIENCE = {
        "CS5100:Foundations of Artificial Intelligence",
        "CS5150:Game Artificial Intelligence",
        "CS5200:Database Management Systems",
        "CS6120:Natural Language Processing",
        "CS6200:Information Retrieval",
        "CS6220:Data Mining Techniques",
        "CS6240:Large-Scale Parallel Data Processing",
        "CS7140:Advanced Machine Learning"
    };
    public static final String[] PROGRAMMING_LANGUAGES = {
        "Python", "Java", "C++", "JavaScript", "C#", "Go", "Swift", "Kotlin", "TypeScript", "C"
    };

    public static final String[] SOFTWARE_PROGRAMMING_SKILLS = {
        "Spring Boot",
        "React.js",
        "Node.js",
        "Express.js",
        "TensorFlow",
        "PyTorch",
        "Hadoop",
        "Spark",
        "Kafka",
        "Docker",
        "Kubernetes",
        "Terraform",
        "Jenkins",
        "Git",
        "AWS"
    };

    public static final String[] OTHER_TOPICS = {
        "Mock Interview",
        "Leetcode",
        "CodeSignal",
        "Resume Review",
        "System Design",
        "Frontend Development",
        "Backend Development",
        "FullStack Development"
    };
}
