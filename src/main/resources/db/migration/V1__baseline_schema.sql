-- ============================================================
-- V1__baseline_schema.sql
-- Baseline schema generated from JPA entities (BaseEntity + 23 tables)
-- Target: PostgreSQL 16
-- ============================================================

-- ---------- company ----------
CREATE TABLE companies (
                           id BIGSERIAL PRIMARY KEY,
                           created_at TIMESTAMP,
                           updated_at TIMESTAMP,
                           created_by VARCHAR(255),
                           updated_by VARCHAR(255),
                           name VARCHAR(150) NOT NULL,
                           logo_url VARCHAR(255),
                           address VARCHAR(255),
                           phone VARCHAR(255),
                           email VARCHAR(255),
                           website VARCHAR(255),
                           kra_pin VARCHAR(20),
                           shif_number VARCHAR(20),
                           nssf_number VARCHAR(20),
                           nita_number VARCHAR(20),
                           currency VARCHAR(10),
                           timezone VARCHAR(50)
);

CREATE TABLE company_settings (
                                  id BIGSERIAL PRIMARY KEY,
                                  created_at TIMESTAMP,
                                  updated_at TIMESTAMP,
                                  created_by VARCHAR(255),
                                  updated_by VARCHAR(255),
                                  company_id BIGINT NOT NULL,
                                  vat_rate NUMERIC(5,2) NOT NULL,
                                  default_working_hours_per_day DOUBLE PRECISION,
                                  overtime_multiplier DOUBLE PRECISION,
                                  weekend_days VARCHAR(20),
                                  payroll_period VARCHAR(20)
);

-- ---------- branch / department ----------
CREATE TABLE branches (
                          id BIGSERIAL PRIMARY KEY,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          created_by VARCHAR(255),
                          updated_by VARCHAR(255),
                          name VARCHAR(100) NOT NULL,
                          code VARCHAR(20) NOT NULL UNIQUE,
                          address VARCHAR(255),
                          phone VARCHAR(255),
                          email VARCHAR(255),
                          manager_id BIGINT,
                          status VARCHAR(20) NOT NULL
);

CREATE TABLE departments (
                             id BIGSERIAL PRIMARY KEY,
                             created_at TIMESTAMP,
                             updated_at TIMESTAMP,
                             created_by VARCHAR(255),
                             updated_by VARCHAR(255),
                             name VARCHAR(100) NOT NULL,
                             branch_id BIGINT NOT NULL,
                             head_of_department_id BIGINT
);

-- ---------- agent ----------
CREATE TABLE agents (
                        id BIGSERIAL PRIMARY KEY,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP,
                        created_by VARCHAR(255),
                        updated_by VARCHAR(255),
                        name VARCHAR(150) NOT NULL,
                        contact_person VARCHAR(255),
                        phone VARCHAR(255),
                        email VARCHAR(255),
                        address VARCHAR(255),
                        bank_name VARCHAR(255),
                        bank_account_number VARCHAR(255),
                        kra_pin VARCHAR(20),
                        contract_start_date DATE,
                        contract_end_date DATE,
                        charge_type VARCHAR(30) NOT NULL,
                        charge_amount NUMERIC(15,2),
                        charge_percentage NUMERIC(5,2),
                        vat_registered BOOLEAN NOT NULL,
                        status VARCHAR(20) NOT NULL
);

CREATE TABLE agent_advances (
                                id BIGSERIAL PRIMARY KEY,
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP,
                                created_by VARCHAR(255),
                                updated_by VARCHAR(255),
                                agent_id BIGINT NOT NULL,
                                amount NUMERIC(15,2) NOT NULL,
                                advance_date DATE NOT NULL,
                                purpose VARCHAR(255),
                                outstanding_balance NUMERIC(15,2) NOT NULL,
                                status VARCHAR(20) NOT NULL
);

-- ---------- employee ----------
CREATE TABLE employees (
                           id BIGSERIAL PRIMARY KEY,
                           created_at TIMESTAMP,
                           updated_at TIMESTAMP,
                           created_by VARCHAR(255),
                           updated_by VARCHAR(255),
                           full_name VARCHAR(150) NOT NULL,
                           employee_number VARCHAR(30) NOT NULL UNIQUE,
                           passport_photo_url VARCHAR(255),
                           national_id VARCHAR(30) UNIQUE,
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
                           employment_status VARCHAR(20) NOT NULL,
                           bank_name VARCHAR(255),
                           bank_account_number VARCHAR(255),
                           emergency_contact_name VARCHAR(255),
                           emergency_contact_phone VARCHAR(255)
);

CREATE TABLE employee_documents (
                                    id BIGSERIAL PRIMARY KEY,
                                    created_at TIMESTAMP,
                                    updated_at TIMESTAMP,
                                    created_by VARCHAR(255),
                                    updated_by VARCHAR(255),
                                    employee_id BIGINT NOT NULL,
                                    document_type VARCHAR(30) NOT NULL,
                                    file_url VARCHAR(255) NOT NULL,
                                    file_name VARCHAR(255)
);

-- ---------- auth ----------
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       created_by VARCHAR(255),
                       updated_by VARCHAR(255),
                       username VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(30) NOT NULL,
                       display_name VARCHAR(150),
                       employee_id BIGINT,
                       agent_id BIGINT,
                       branch_id BIGINT,
                       active BOOLEAN NOT NULL,
                       must_change_password BOOLEAN NOT NULL
);

-- ---------- attendance / biometrics ----------
CREATE TABLE attendance (
                            id BIGSERIAL PRIMARY KEY,
                            created_at TIMESTAMP,
                            updated_at TIMESTAMP,
                            created_by VARCHAR(255),
                            updated_by VARCHAR(255),
                            employee_id BIGINT NOT NULL,
                            branch_id BIGINT NOT NULL,
                            attendance_date DATE NOT NULL,
                            clock_in TIMESTAMP,
                            clock_out TIMESTAMP,
                            source VARCHAR(20) NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            event_id VARCHAR(100) UNIQUE,
                            device_id BIGINT,
                            match_confidence_score DOUBLE PRECISION
);

CREATE TABLE biometric_devices (
                                   id BIGSERIAL PRIMARY KEY,
                                   created_at TIMESTAMP,
                                   updated_at TIMESTAMP,
                                   created_by VARCHAR(255),
                                   updated_by VARCHAR(255),
                                   branch_id BIGINT NOT NULL,
                                   device_serial VARCHAR(100) NOT NULL UNIQUE,
                                   device_token_hash VARCHAR(255) NOT NULL,
                                   last_sync_at TIMESTAMP,
                                   status VARCHAR(20) NOT NULL
);

CREATE TABLE biometric_templates (
                                     id BIGSERIAL PRIMARY KEY,
                                     created_at TIMESTAMP,
                                     updated_at TIMESTAMP,
                                     created_by VARCHAR(255),
                                     updated_by VARCHAR(255),
                                     employee_id BIGINT NOT NULL,
                                     branch_id BIGINT NOT NULL,
                                     template_data BYTEA NOT NULL,
                                     finger_index INTEGER NOT NULL,
                                     quality_score DOUBLE PRECISION,
                                     enrolled_at TIMESTAMP NOT NULL
);

-- ---------- shift ----------
CREATE TABLE shifts (
                        id BIGSERIAL PRIMARY KEY,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP,
                        created_by VARCHAR(255),
                        updated_by VARCHAR(255),
                        name VARCHAR(100) NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        start_time TIME,
                        end_time TIME
);

CREATE TABLE shift_assignments (
                                   id BIGSERIAL PRIMARY KEY,
                                   created_at TIMESTAMP,
                                   updated_at TIMESTAMP,
                                   created_by VARCHAR(255),
                                   updated_by VARCHAR(255),
                                   shift_id BIGINT NOT NULL,
                                   employee_id BIGINT,
                                   department_id BIGINT,
                                   branch_id BIGINT
);

-- ---------- leave ----------
CREATE TABLE leave_types (
                             id BIGSERIAL PRIMARY KEY,
                             created_at TIMESTAMP,
                             updated_at TIMESTAMP,
                             created_by VARCHAR(255),
                             updated_by VARCHAR(255),
                             name VARCHAR(50) NOT NULL UNIQUE,
                             default_entitlement_days INTEGER,
                             paid BOOLEAN NOT NULL
);

CREATE TABLE leave_requests (
                                id BIGSERIAL PRIMARY KEY,
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP,
                                created_by VARCHAR(255),
                                updated_by VARCHAR(255),
                                employee_id BIGINT NOT NULL,
                                leave_type_id BIGINT NOT NULL,
                                start_date DATE NOT NULL,
                                end_date DATE NOT NULL,
                                reason VARCHAR(255),
                                status VARCHAR(20) NOT NULL,
                                approved_by_manager_id BIGINT,
                                approved_by_hr_id BIGINT,
                                rejection_reason VARCHAR(255)
);

-- ---------- loan ----------
CREATE TABLE loans (
                       id BIGSERIAL PRIMARY KEY,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       created_by VARCHAR(255),
                       updated_by VARCHAR(255),
                       employee_id BIGINT NOT NULL,
                       principal_amount NUMERIC(15,2) NOT NULL,
                       interest_rate NUMERIC(5,2),
                       installment_amount NUMERIC(15,2) NOT NULL,
                       outstanding_balance NUMERIC(15,2) NOT NULL,
                       request_date DATE,
                       approval_date DATE,
                       status VARCHAR(20) NOT NULL
);

CREATE TABLE loan_repayments (
                                 id BIGSERIAL PRIMARY KEY,
                                 created_at TIMESTAMP,
                                 updated_at TIMESTAMP,
                                 created_by VARCHAR(255),
                                 updated_by VARCHAR(255),
                                 loan_id BIGINT NOT NULL,
                                 payroll_run_id BIGINT NOT NULL,
                                 amount_paid NUMERIC(15,2) NOT NULL
);

-- ---------- payroll ----------
CREATE TABLE payroll_runs (
                              id BIGSERIAL PRIMARY KEY,
                              created_at TIMESTAMP,
                              updated_at TIMESTAMP,
                              created_by VARCHAR(255),
                              updated_by VARCHAR(255),
                              period_start DATE NOT NULL,
                              period_end DATE NOT NULL,
                              scope_branch_ids VARCHAR(255),
                              scope_agent_ids VARCHAR(255),
                              scope_department_ids VARCHAR(255),
                              status VARCHAR(20) NOT NULL,
                              total_gross_payroll NUMERIC(15,2),
                              total_net_payroll NUMERIC(15,2),
                              initiated_by_user_id BIGINT
);

CREATE TABLE payslips (
                          id BIGSERIAL PRIMARY KEY,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          created_by VARCHAR(255),
                          updated_by VARCHAR(255),
                          payroll_run_id BIGINT NOT NULL,
                          employee_id BIGINT NOT NULL,
                          basic_salary NUMERIC(15,2) NOT NULL,
                          house_allowance NUMERIC(15,2),
                          transport_allowance NUMERIC(15,2),
                          overtime_pay NUMERIC(15,2),
                          bonus NUMERIC(15,2),
                          other_allowances NUMERIC(15,2),
                          gross_pay NUMERIC(15,2) NOT NULL,
                          shif_deduction NUMERIC(15,2),
                          nssf_deduction NUMERIC(15,2),
                          housing_levy_deduction NUMERIC(15,2),
                          paye_deduction NUMERIC(15,2),
                          nita_deduction NUMERIC(15,2),
                          loan_deduction NUMERIC(15,2),
                          other_deductions NUMERIC(15,2),
                          net_pay NUMERIC(15,2) NOT NULL
);

CREATE TABLE agent_payroll_summaries (
                                         id BIGSERIAL PRIMARY KEY,
                                         created_at TIMESTAMP,
                                         updated_at TIMESTAMP,
                                         created_by VARCHAR(255),
                                         updated_by VARCHAR(255),
                                         payroll_run_id BIGINT NOT NULL,
                                         agent_id BIGINT NOT NULL,
                                         employee_count INTEGER,
                                         gross_salaries NUMERIC(15,2) NOT NULL,
                                         service_charge NUMERIC(15,2) NOT NULL,
                                         shif_total NUMERIC(15,2),
                                         nssf_total NUMERIC(15,2),
                                         housing_levy_total NUMERIC(15,2),
                                         paye_total NUMERIC(15,2),
                                         employee_loan_deductions_total NUMERIC(15,2),
                                         advance_recovery_total NUMERIC(15,2),
                                         subtotal_before_vat NUMERIC(15,2) NOT NULL,
                                         vat_applied BOOLEAN NOT NULL,
                                         vat_rate_used NUMERIC(5,2),
                                         vat_amount NUMERIC(15,2),
                                         final_amount_payable NUMERIC(15,2) NOT NULL
);

-- ---------- notification / audit ----------
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               created_at TIMESTAMP,
                               updated_at TIMESTAMP,
                               created_by VARCHAR(255),
                               updated_by VARCHAR(255),
                               recipient_user_id BIGINT NOT NULL,
                               channel VARCHAR(30) NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               message VARCHAR(255) NOT NULL,
                               read BOOLEAN NOT NULL
);

CREATE TABLE audit_logs (
                            id BIGSERIAL PRIMARY KEY,
                            created_at TIMESTAMP,
                            updated_at TIMESTAMP,
                            created_by VARCHAR(255),
                            updated_by VARCHAR(255),
                            performed_by_user_id BIGINT,
                            action VARCHAR(100) NOT NULL,
                            entity_type VARCHAR(50) NOT NULL,
                            entity_id BIGINT,
                            details_json TEXT
);

-- ---------- helpful indexes on FK-style columns ----------
CREATE INDEX idx_agent_advances_agent_id ON agent_advances(agent_id);
CREATE INDEX idx_attendance_employee_id ON attendance(employee_id);
CREATE INDEX idx_attendance_branch_id ON attendance(branch_id);
CREATE INDEX idx_biometric_templates_employee_id ON biometric_templates(employee_id);
CREATE INDEX idx_departments_branch_id ON departments(branch_id);
CREATE INDEX idx_employees_branch_id ON employees(branch_id);
CREATE INDEX idx_employees_agent_id ON employees(agent_id);
CREATE INDEX idx_leave_requests_employee_id ON leave_requests(employee_id);
CREATE INDEX idx_loans_employee_id ON loans(employee_id);
CREATE INDEX idx_loan_repayments_loan_id ON loan_repayments(loan_id);
CREATE INDEX idx_notifications_recipient_user_id ON notifications(recipient_user_id);
CREATE INDEX idx_payslips_payroll_run_id ON payslips(payroll_run_id);
CREATE INDEX idx_payslips_employee_id ON payslips(employee_id);
CREATE INDEX idx_shift_assignments_shift_id ON shift_assignments(shift_id);
CREATE INDEX idx_users_username ON users(username);