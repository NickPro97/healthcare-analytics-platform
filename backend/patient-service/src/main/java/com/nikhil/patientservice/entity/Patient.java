package com.nikhil.patientservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(name = "patients", uniqueConstraints = @UniqueConstraint(name = "uk_patients_mrn", columnNames = "mrn"))
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    private String name;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "M|F|O|U", message = "gender must be M, F, O or U")
    private String gender;

    /** Medical Record Number. For MIMIC demo data this is "MIMIC-" + SUBJECT_ID. */
    @NotBlank
    @Size(max = 40)
    private String mrn;

    public Patient() {
    }

    public Patient(String name, LocalDate dateOfBirth, String gender, String mrn) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.mrn = mrn;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }
}
