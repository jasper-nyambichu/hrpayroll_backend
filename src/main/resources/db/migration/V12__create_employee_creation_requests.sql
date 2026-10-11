-- HR submits a new-employee request; an Admin approves it, which creates the employee.
CREATE TABLE employee_creation_requests (
                                            id BIGSERIAL PRIMARY KEY,
                                            created_at TIMESTAMP,
                                            updated_at TIMESTAMP,
                                            created_by VARCHAR(255),
                                            updated_by VARCHAR(255),

                                            status VARCHAR(20) NOT NULL,
                                            requested_by_user_id BIGINT NOT NULL,
                                            reviewed_by_user_id BIGINT,
                                            reviewed_at TIMESTAMP,
                                            rejection_reason VARCHAR(500),
                                            created_employee_id BIGINT,

                                            full_name VARCHAR(150) NOT NULL,
                                            national_id VARCHAR(30),
                                            date_of_birth DATE,
                                            gender VARCHAR(10),
                                            marital_status VARCHAR(20),
                                            phone VARCHAR(255),
                                            email VARCHAR(255),
                                            address VARCHAR(255),
                                            kra_pin VARCHAR(20),
                                            shif_number VARCHAR(20),
                                            nssf_number VARCHAR(20),
                                            nita_number VARCHAR(20),
                                            branch_id BIGINT NOT NULL,
                                            department_id BIGINT,
                                            agent_id BIGINT,
                                            position VARCHAR(255),
                                            employment_type VARCHAR(20) NOT NULL,
                                            basic_salary NUMERIC(15,2) NOT NULL,
                                            date_hired DATE,
                                            bank_name VARCHAR(255),
                                            bank_account_number VARCHAR(255),
                                            emergency_contact_name VARCHAR(255),
                                            emergency_contact_phone VARCHAR(255)
);

CREATE INDEX idx_ecr_status ON employee_creation_requests(status);
CREATE INDEX idx_ecr_requested_by ON employee_creation_requests(requested_by_user_id);
CREATE INDEX idx_ecr_national_id ON employee_creation_requests(national_id);