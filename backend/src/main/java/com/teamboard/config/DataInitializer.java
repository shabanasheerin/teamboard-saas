package com.teamboard.config;

import com.teamboard.entity.*;
import com.teamboard.repository.OrganizationRepository;
import com.teamboard.repository.ProjectRepository;
import com.teamboard.repository.TaskRepository;
import com.teamboard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Local and demo only (app.seed-demo-data=true).
 * Creates TWO companies so you can see that one company never sees the other's data.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;
    private final boolean seed;

    public DataInitializer(OrganizationRepository organizations, UserRepository users, ProjectRepository projects,
                           TaskRepository tasks, PasswordEncoder encoder,
                           @Value("${app.seed-demo-data:false}") boolean seed) {
        this.organizations = organizations;
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.encoder = encoder;
        this.seed = seed;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seed || organizations.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();

        // ---- Company 1: Acme Technologies ----
        Organization acme = org("Acme Technologies", "acme-technologies");
        User acmeAdmin = user(acme, "Asha Admin", "admin@acme.com", "Admin@123", Role.ADMIN);
        User acmeManager = user(acme, "Priya Manager", "manager@acme.com", "Manager@123", Role.MANAGER);
        User ravi = user(acme, "Ravi Kumar", "ravi@acme.com", "Member@123", Role.MEMBER);
        User anitha = user(acme, "Anitha S", "anitha@acme.com", "Member@123", Role.MEMBER);

        Project web = project(acme, "Website Revamp", "WEB", "New company website", acmeManager);
        task(web, "Design the home page", "Hero section, navigation and footer.", TaskStatus.DONE, Priority.HIGH, today.minusDays(4), ravi, acmeManager);
        task(web, "Build the login page", "Email and password form with validation.", TaskStatus.IN_PROGRESS, Priority.HIGH, today.plusDays(2), ravi, acmeManager);
        task(web, "Write the pricing copy", "Three plans: Starter, Team, Business.", TaskStatus.IN_REVIEW, Priority.MEDIUM, today.plusDays(1), anitha, acmeManager);
        task(web, "Fix mobile menu bug", "Menu does not close after tapping a link.", TaskStatus.TODO, Priority.URGENT, today.minusDays(1), anitha, acmeManager);
        task(web, "Add analytics", "Track page views and sign-ups.", TaskStatus.TODO, Priority.LOW, today.plusDays(9), null, acmeAdmin);

        Project mobile = project(acme, "Mobile App", "MOB", "Customer mobile app", acmeManager);
        task(mobile, "Set up push notifications", "Android and iOS.", TaskStatus.TODO, Priority.MEDIUM, today.plusDays(6), ravi, acmeManager);
        task(mobile, "Offline mode research", "Compare local storage options.", TaskStatus.IN_PROGRESS, Priority.LOW, today.plusDays(12), anitha, acmeManager);

        // ---- Company 2: Globex Corp (cannot see anything from Acme) ----
        Organization globex = org("Globex Corp", "globex-corp");
        User globexAdmin = user(globex, "Gita Admin", "admin@globex.com", "Admin@123", Role.ADMIN);
        User globexMember = user(globex, "Gopal Member", "gopal@globex.com", "Member@123", Role.MEMBER);
        Project tools = project(globex, "Internal Tools", "INT", "Tools for the support team", globexAdmin);
        task(tools, "Build a ticket dashboard", "Show open tickets by team.", TaskStatus.IN_PROGRESS, Priority.HIGH, today.plusDays(3), globexMember, globexAdmin);

        log.info("Demo data created: Acme Technologies and Globex Corp");
    }

    private Organization org(String name, String slug) {
        Organization o = new Organization();
        o.setName(name);
        o.setSlug(slug);
        return organizations.save(o);
    }

    private User user(Organization org, String name, String email, String password, Role role) {
        User u = new User();
        u.setOrganization(org);
        u.setName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setRole(role);
        return users.save(u);
    }

    private Project project(Organization org, String name, String key, String description, User createdBy) {
        Project p = new Project();
        p.setOrganization(org);
        p.setName(name);
        p.setProjectKey(key);
        p.setDescription(description);
        p.setCreatedBy(createdBy);
        return projects.save(p);
    }

    private void task(Project project, String title, String description, TaskStatus status, Priority priority,
                      LocalDate due, User assignee, User reporter) {
        project.setTaskCounter(project.getTaskCounter() + 1);
        Task t = new Task();
        t.setOrganization(project.getOrganization());
        t.setProject(project);
        t.setTaskNumber(project.getTaskCounter());
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setPriority(priority);
        t.setDueDate(due);
        t.setAssignee(assignee);
        t.setReporter(reporter);
        tasks.save(t);
        projects.save(project);
    }
}
