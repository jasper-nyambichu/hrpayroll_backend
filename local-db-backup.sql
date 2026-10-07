--
-- PostgreSQL database dump
--

\restrict 02WGMA1bnopmASYqdvem8Qz4lPZzbFwsHIZspPadEcXfnQmJxvjY1PQIIo2AWFU

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: agent_advances; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.agent_advances (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    agent_id bigint NOT NULL,
    amount numeric(15,2) NOT NULL,
    advance_date date NOT NULL,
    purpose character varying(255),
    outstanding_balance numeric(15,2) NOT NULL,
    status character varying(20) NOT NULL
);


ALTER TABLE public.agent_advances OWNER TO hrpayroll_user;

--
-- Name: agent_advances_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.agent_advances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.agent_advances_id_seq OWNER TO hrpayroll_user;

--
-- Name: agent_advances_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.agent_advances_id_seq OWNED BY public.agent_advances.id;


--
-- Name: agent_payroll_summaries; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.agent_payroll_summaries (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    payroll_run_id bigint NOT NULL,
    agent_id bigint NOT NULL,
    employee_count integer,
    gross_salaries numeric(15,2) NOT NULL,
    service_charge numeric(15,2) NOT NULL,
    shif_total numeric(15,2),
    nssf_total numeric(15,2),
    housing_levy_total numeric(15,2),
    paye_total numeric(15,2),
    employee_loan_deductions_total numeric(15,2),
    advance_recovery_total numeric(15,2),
    subtotal_before_vat numeric(15,2) NOT NULL,
    vat_applied boolean NOT NULL,
    vat_rate_used numeric(5,2),
    vat_amount numeric(15,2),
    final_amount_payable numeric(15,2) NOT NULL
);


ALTER TABLE public.agent_payroll_summaries OWNER TO hrpayroll_user;

--
-- Name: agent_payroll_summaries_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.agent_payroll_summaries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.agent_payroll_summaries_id_seq OWNER TO hrpayroll_user;

--
-- Name: agent_payroll_summaries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.agent_payroll_summaries_id_seq OWNED BY public.agent_payroll_summaries.id;


--
-- Name: agents; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.agents (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    name character varying(150) NOT NULL,
    contact_person character varying(255),
    phone character varying(255),
    email character varying(255),
    address character varying(255),
    bank_name character varying(255),
    bank_account_number character varying(255),
    kra_pin character varying(20),
    contract_start_date date,
    contract_end_date date,
    charge_type character varying(30) NOT NULL,
    charge_amount numeric(15,2),
    charge_percentage numeric(5,2),
    vat_registered boolean NOT NULL,
    status character varying(20) NOT NULL
);


ALTER TABLE public.agents OWNER TO hrpayroll_user;

--
-- Name: agents_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.agents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.agents_id_seq OWNER TO hrpayroll_user;

--
-- Name: agents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.agents_id_seq OWNED BY public.agents.id;


--
-- Name: attendance; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.attendance (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    employee_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    attendance_date date NOT NULL,
    clock_in timestamp without time zone,
    clock_out timestamp without time zone,
    source character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    event_id character varying(100),
    device_id bigint,
    verify_method character varying(30),
    clock_out_event_id character varying(100)
);


ALTER TABLE public.attendance OWNER TO hrpayroll_user;

--
-- Name: attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.attendance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.attendance_id_seq OWNER TO hrpayroll_user;

--
-- Name: attendance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.attendance_id_seq OWNED BY public.attendance.id;


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    performed_by_user_id bigint,
    action character varying(100) NOT NULL,
    entity_type character varying(50) NOT NULL,
    entity_id bigint,
    details_json oid
);


ALTER TABLE public.audit_logs OWNER TO hrpayroll_user;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.audit_logs_id_seq OWNER TO hrpayroll_user;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: biometric_device_mappings; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.biometric_device_mappings (
    id bigint NOT NULL,
    device_id bigint NOT NULL,
    terminal_user_id character varying(20) NOT NULL,
    employee_id bigint NOT NULL,
    enrolled_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(255),
    updated_by character varying(255)
);


ALTER TABLE public.biometric_device_mappings OWNER TO hrpayroll_user;

--
-- Name: biometric_device_mappings_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.biometric_device_mappings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.biometric_device_mappings_id_seq OWNER TO hrpayroll_user;

--
-- Name: biometric_device_mappings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.biometric_device_mappings_id_seq OWNED BY public.biometric_device_mappings.id;


--
-- Name: biometric_devices; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.biometric_devices (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    branch_id bigint NOT NULL,
    device_serial character varying(100) NOT NULL,
    device_token_hash character varying(255) NOT NULL,
    last_sync_at timestamp without time zone,
    status character varying(20) NOT NULL
);


ALTER TABLE public.biometric_devices OWNER TO hrpayroll_user;

--
-- Name: biometric_devices_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.biometric_devices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.biometric_devices_id_seq OWNER TO hrpayroll_user;

--
-- Name: biometric_devices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.biometric_devices_id_seq OWNED BY public.biometric_devices.id;


--
-- Name: biometric_sync_failures; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.biometric_sync_failures (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    event_id character varying(100),
    device_serial character varying(100),
    employee_id bigint,
    reason character varying(255) NOT NULL
);


ALTER TABLE public.biometric_sync_failures OWNER TO hrpayroll_user;

--
-- Name: biometric_sync_failures_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.biometric_sync_failures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.biometric_sync_failures_id_seq OWNER TO hrpayroll_user;

--
-- Name: biometric_sync_failures_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.biometric_sync_failures_id_seq OWNED BY public.biometric_sync_failures.id;


--
-- Name: biometric_templates; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.biometric_templates (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    employee_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    template_data bytea NOT NULL,
    finger_index integer NOT NULL,
    quality_score double precision,
    enrolled_at timestamp without time zone NOT NULL
);


ALTER TABLE public.biometric_templates OWNER TO hrpayroll_user;

--
-- Name: biometric_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.biometric_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.biometric_templates_id_seq OWNER TO hrpayroll_user;

--
-- Name: biometric_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.biometric_templates_id_seq OWNED BY public.biometric_templates.id;


--
-- Name: branches; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.branches (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    name character varying(100) NOT NULL,
    code character varying(20) NOT NULL,
    address character varying(255),
    phone character varying(255),
    email character varying(255),
    manager_id bigint,
    status character varying(20) NOT NULL
);


ALTER TABLE public.branches OWNER TO hrpayroll_user;

--
-- Name: branches_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.branches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.branches_id_seq OWNER TO hrpayroll_user;

--
-- Name: branches_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.branches_id_seq OWNED BY public.branches.id;


--
-- Name: companies; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.companies (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    name character varying(150) NOT NULL,
    logo_url character varying(255),
    address character varying(255),
    phone character varying(255),
    email character varying(255),
    website character varying(255),
    kra_pin character varying(20),
    shif_number character varying(20),
    nssf_number character varying(20),
    nita_number character varying(20),
    currency character varying(10),
    timezone character varying(50)
);


ALTER TABLE public.companies OWNER TO hrpayroll_user;

--
-- Name: companies_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.companies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.companies_id_seq OWNER TO hrpayroll_user;

--
-- Name: companies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.companies_id_seq OWNED BY public.companies.id;


--
-- Name: company_settings; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.company_settings (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    company_id bigint NOT NULL,
    vat_rate numeric(5,2) NOT NULL,
    default_working_hours_per_day double precision,
    overtime_multiplier double precision,
    weekend_days character varying(20),
    payroll_period character varying(20)
);


ALTER TABLE public.company_settings OWNER TO hrpayroll_user;

--
-- Name: company_settings_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.company_settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.company_settings_id_seq OWNER TO hrpayroll_user;

--
-- Name: company_settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.company_settings_id_seq OWNED BY public.company_settings.id;


--
-- Name: departments; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.departments (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    name character varying(100) NOT NULL,
    branch_id bigint NOT NULL,
    head_of_department_id bigint
);


ALTER TABLE public.departments OWNER TO hrpayroll_user;

--
-- Name: departments_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.departments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.departments_id_seq OWNER TO hrpayroll_user;

--
-- Name: departments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.departments_id_seq OWNED BY public.departments.id;


--
-- Name: employee_documents; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.employee_documents (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    employee_id bigint NOT NULL,
    document_type character varying(30) NOT NULL,
    file_url character varying(255) NOT NULL,
    file_name character varying(255)
);


ALTER TABLE public.employee_documents OWNER TO hrpayroll_user;

--
-- Name: employee_documents_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.employee_documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.employee_documents_id_seq OWNER TO hrpayroll_user;

--
-- Name: employee_documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.employee_documents_id_seq OWNED BY public.employee_documents.id;


--
-- Name: employees; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.employees (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    full_name character varying(150) NOT NULL,
    employee_number character varying(30) NOT NULL,
    passport_photo_url character varying(255),
    national_id character varying(30),
    date_of_birth date,
    gender character varying(10),
    marital_status character varying(20),
    phone character varying(255),
    email character varying(255),
    address character varying(255),
    kra_pin character varying(20),
    shif_number character varying(20),
    nssf_number character varying(20),
    nita_number character varying(20),
    branch_id bigint NOT NULL,
    department_id bigint,
    agent_id bigint,
    "position" character varying(255),
    employment_type character varying(20) NOT NULL,
    basic_salary numeric(15,2) NOT NULL,
    date_hired date,
    employment_status character varying(20) NOT NULL,
    bank_name character varying(255),
    bank_account_number character varying(255),
    emergency_contact_name character varying(255),
    emergency_contact_phone character varying(255)
);


ALTER TABLE public.employees OWNER TO hrpayroll_user;

--
-- Name: employees_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.employees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.employees_id_seq OWNER TO hrpayroll_user;

--
-- Name: employees_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.employees_id_seq OWNED BY public.employees.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO hrpayroll_user;

--
-- Name: leave_requests; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.leave_requests (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    employee_id bigint NOT NULL,
    leave_type_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    reason character varying(255),
    status character varying(20) NOT NULL,
    approved_by_manager_id bigint,
    approved_by_hr_id bigint,
    rejection_reason character varying(255)
);


ALTER TABLE public.leave_requests OWNER TO hrpayroll_user;

--
-- Name: leave_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.leave_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.leave_requests_id_seq OWNER TO hrpayroll_user;

--
-- Name: leave_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.leave_requests_id_seq OWNED BY public.leave_requests.id;


--
-- Name: leave_types; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.leave_types (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    name character varying(50) NOT NULL,
    default_entitlement_days integer,
    paid boolean NOT NULL
);


ALTER TABLE public.leave_types OWNER TO hrpayroll_user;

--
-- Name: leave_types_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.leave_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.leave_types_id_seq OWNER TO hrpayroll_user;

--
-- Name: leave_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.leave_types_id_seq OWNED BY public.leave_types.id;


--
-- Name: loan_repayments; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.loan_repayments (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    loan_id bigint NOT NULL,
    payroll_run_id bigint NOT NULL,
    amount_paid numeric(15,2) NOT NULL
);


ALTER TABLE public.loan_repayments OWNER TO hrpayroll_user;

--
-- Name: loan_repayments_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.loan_repayments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.loan_repayments_id_seq OWNER TO hrpayroll_user;

--
-- Name: loan_repayments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.loan_repayments_id_seq OWNED BY public.loan_repayments.id;


--
-- Name: loans; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.loans (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    employee_id bigint NOT NULL,
    principal_amount numeric(15,2) NOT NULL,
    interest_rate numeric(5,2),
    installment_amount numeric(15,2) NOT NULL,
    outstanding_balance numeric(15,2) NOT NULL,
    request_date date,
    approval_date date,
    status character varying(20) NOT NULL
);


ALTER TABLE public.loans OWNER TO hrpayroll_user;

--
-- Name: loans_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.loans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.loans_id_seq OWNER TO hrpayroll_user;

--
-- Name: loans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.loans_id_seq OWNED BY public.loans.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    recipient_user_id bigint NOT NULL,
    channel character varying(30) NOT NULL,
    type character varying(50) NOT NULL,
    message character varying(255) NOT NULL,
    read boolean NOT NULL
);


ALTER TABLE public.notifications OWNER TO hrpayroll_user;

--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notifications_id_seq OWNER TO hrpayroll_user;

--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: payroll_runs; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.payroll_runs (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    period_start date NOT NULL,
    period_end date NOT NULL,
    scope_branch_ids character varying(255),
    scope_agent_ids character varying(255),
    scope_department_ids character varying(255),
    status character varying(20) NOT NULL,
    total_gross_payroll numeric(15,2),
    total_net_payroll numeric(15,2),
    initiated_by_user_id bigint
);


ALTER TABLE public.payroll_runs OWNER TO hrpayroll_user;

--
-- Name: payroll_runs_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.payroll_runs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payroll_runs_id_seq OWNER TO hrpayroll_user;

--
-- Name: payroll_runs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.payroll_runs_id_seq OWNED BY public.payroll_runs.id;


--
-- Name: payslips; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.payslips (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    payroll_run_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    basic_salary numeric(15,2) NOT NULL,
    house_allowance numeric(15,2),
    transport_allowance numeric(15,2),
    overtime_pay numeric(15,2),
    bonus numeric(15,2),
    other_allowances numeric(15,2),
    gross_pay numeric(15,2) NOT NULL,
    shif_deduction numeric(15,2),
    nssf_deduction numeric(15,2),
    housing_levy_deduction numeric(15,2),
    paye_deduction numeric(15,2),
    nita_deduction numeric(15,2),
    loan_deduction numeric(15,2),
    other_deductions numeric(15,2),
    net_pay numeric(15,2) NOT NULL
);


ALTER TABLE public.payslips OWNER TO hrpayroll_user;

--
-- Name: payslips_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.payslips_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payslips_id_seq OWNER TO hrpayroll_user;

--
-- Name: payslips_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.payslips_id_seq OWNED BY public.payslips.id;


--
-- Name: shift_assignments; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.shift_assignments (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    shift_id bigint NOT NULL,
    employee_id bigint,
    department_id bigint,
    branch_id bigint
);


ALTER TABLE public.shift_assignments OWNER TO hrpayroll_user;

--
-- Name: shift_assignments_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.shift_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.shift_assignments_id_seq OWNER TO hrpayroll_user;

--
-- Name: shift_assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.shift_assignments_id_seq OWNED BY public.shift_assignments.id;


--
-- Name: shifts; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.shifts (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    name character varying(100) NOT NULL,
    type character varying(20) NOT NULL,
    start_time time without time zone,
    end_time time without time zone
);


ALTER TABLE public.shifts OWNER TO hrpayroll_user;

--
-- Name: shifts_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.shifts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.shifts_id_seq OWNER TO hrpayroll_user;

--
-- Name: shifts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.shifts_id_seq OWNED BY public.shifts.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: hrpayroll_user
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    created_by character varying(255),
    updated_by character varying(255),
    username character varying(100) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(30) NOT NULL,
    display_name character varying(150),
    employee_id bigint,
    agent_id bigint,
    branch_id bigint,
    active boolean NOT NULL,
    must_change_password boolean NOT NULL
);


ALTER TABLE public.users OWNER TO hrpayroll_user;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: hrpayroll_user
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO hrpayroll_user;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: hrpayroll_user
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: agent_advances id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.agent_advances ALTER COLUMN id SET DEFAULT nextval('public.agent_advances_id_seq'::regclass);


--
-- Name: agent_payroll_summaries id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.agent_payroll_summaries ALTER COLUMN id SET DEFAULT nextval('public.agent_payroll_summaries_id_seq'::regclass);


--
-- Name: agents id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.agents ALTER COLUMN id SET DEFAULT nextval('public.agents_id_seq'::regclass);


--
-- Name: attendance id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.attendance ALTER COLUMN id SET DEFAULT nextval('public.attendance_id_seq'::regclass);


--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: biometric_device_mappings id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_device_mappings ALTER COLUMN id SET DEFAULT nextval('public.biometric_device_mappings_id_seq'::regclass);


--
-- Name: biometric_devices id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_devices ALTER COLUMN id SET DEFAULT nextval('public.biometric_devices_id_seq'::regclass);


--
-- Name: biometric_sync_failures id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_sync_failures ALTER COLUMN id SET DEFAULT nextval('public.biometric_sync_failures_id_seq'::regclass);


--
-- Name: biometric_templates id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_templates ALTER COLUMN id SET DEFAULT nextval('public.biometric_templates_id_seq'::regclass);


--
-- Name: branches id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.branches ALTER COLUMN id SET DEFAULT nextval('public.branches_id_seq'::regclass);


--
-- Name: companies id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq'::regclass);


--
-- Name: company_settings id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.company_settings ALTER COLUMN id SET DEFAULT nextval('public.company_settings_id_seq'::regclass);


--
-- Name: departments id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.departments ALTER COLUMN id SET DEFAULT nextval('public.departments_id_seq'::regclass);


--
-- Name: employee_documents id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.employee_documents ALTER COLUMN id SET DEFAULT nextval('public.employee_documents_id_seq'::regclass);


--
-- Name: employees id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.employees ALTER COLUMN id SET DEFAULT nextval('public.employees_id_seq'::regclass);


--
-- Name: leave_requests id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.leave_requests ALTER COLUMN id SET DEFAULT nextval('public.leave_requests_id_seq'::regclass);


--
-- Name: leave_types id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.leave_types ALTER COLUMN id SET DEFAULT nextval('public.leave_types_id_seq'::regclass);


--
-- Name: loan_repayments id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.loan_repayments ALTER COLUMN id SET DEFAULT nextval('public.loan_repayments_id_seq'::regclass);


--
-- Name: loans id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.loans ALTER COLUMN id SET DEFAULT nextval('public.loans_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: payroll_runs id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.payroll_runs ALTER COLUMN id SET DEFAULT nextval('public.payroll_runs_id_seq'::regclass);


--
-- Name: payslips id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.payslips ALTER COLUMN id SET DEFAULT nextval('public.payslips_id_seq'::regclass);


--
-- Name: shift_assignments id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.shift_assignments ALTER COLUMN id SET DEFAULT nextval('public.shift_assignments_id_seq'::regclass);


--
-- Name: shifts id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.shifts ALTER COLUMN id SET DEFAULT nextval('public.shifts_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: agent_advances; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.agent_advances (id, created_at, updated_at, created_by, updated_by, agent_id, amount, advance_date, purpose, outstanding_balance, status) FROM stdin;
\.


--
-- Data for Name: agent_payroll_summaries; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.agent_payroll_summaries (id, created_at, updated_at, created_by, updated_by, payroll_run_id, agent_id, employee_count, gross_salaries, service_charge, shif_total, nssf_total, housing_levy_total, paye_total, employee_loan_deductions_total, advance_recovery_total, subtotal_before_vat, vat_applied, vat_rate_used, vat_amount, final_amount_payable) FROM stdin;
\.


--
-- Data for Name: agents; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.agents (id, created_at, updated_at, created_by, updated_by, name, contact_person, phone, email, address, bank_name, bank_account_number, kra_pin, contract_start_date, contract_end_date, charge_type, charge_amount, charge_percentage, vat_registered, status) FROM stdin;
1	2026-09-20 07:59:50.839892	2026-09-20 07:59:50.839892	\N	\N	Test Agent Ltd	\N	\N	\N	\N	\N	\N	\N	\N	\N	FIXED_MONTHLY	5000.00	\N	f	ACTIVE
\.


--
-- Data for Name: attendance; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.attendance (id, created_at, updated_at, created_by, updated_by, employee_id, branch_id, attendance_date, clock_in, clock_out, source, status, event_id, device_id, verify_method, clock_out_event_id) FROM stdin;
1	2026-09-28 12:32:48.309765	2026-09-28 12:32:48.309765	\N	\N	2	1	2026-09-28	2026-09-28 15:32:31.59343	\N	BIOMETRIC	PRESENT	secugen-evt-64dd08d6a9c14321a8eb26fb8c3cd1d1	3	FINGERPRINT	\N
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.audit_logs (id, created_at, updated_at, created_by, updated_by, performed_by_user_id, action, entity_type, entity_id, details_json) FROM stdin;
1	2026-09-23 11:09:58.488864	2026-09-23 11:09:58.488864	\N	\N	1	BIOMETRIC_DEVICE_REGISTERED	BiometricDevice	\N	\N
2	2026-09-23 11:34:12.855018	2026-09-23 11:34:12.855018	\N	\N	1	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
3	2026-09-24 07:48:17.590682	2026-09-24 07:48:17.590682	\N	\N	1	BIOMETRIC_DEVICE_REGISTERED	BiometricDevice	\N	\N
4	2026-09-24 07:52:19.29091	2026-09-24 07:52:19.29091	\N	\N	1	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
5	2026-09-24 08:15:16.822808	2026-09-24 08:15:16.822808	\N	\N	1	BIOMETRIC_DEVICE_DEACTIVATED	BiometricDevice	\N	\N
6	2026-09-24 08:15:52.980728	2026-09-24 08:15:52.980728	\N	\N	1	BIOMETRIC_DEVICE_DEACTIVATED	BiometricDevice	\N	\N
7	2026-09-24 08:16:45.530536	2026-09-24 08:16:45.530536	\N	\N	1	BIOMETRIC_DEVICE_REGISTERED	BiometricDevice	\N	\N
8	2026-09-24 08:18:35.409864	2026-09-24 08:18:35.409864	\N	\N	1	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
9	2026-09-28 09:21:21.718052	2026-09-28 09:21:21.718052	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
10	2026-09-28 09:32:57.360294	2026-09-28 09:32:57.360294	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
11	2026-09-28 11:14:17.852071	2026-09-28 11:14:17.852071	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
12	2026-09-29 13:19:31.609173	2026-09-29 13:19:31.609173	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
13	2026-09-29 13:19:46.901871	2026-09-29 13:19:46.901871	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
14	2026-09-29 13:20:42.881961	2026-09-29 13:20:42.881961	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
15	2026-09-29 13:29:39.094598	2026-09-29 13:29:39.094598	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
16	2026-09-29 17:17:41.772344	2026-09-29 17:17:41.772344	\N	\N	4	BIOMETRIC_TERMINAL_MAPPED	BiometricDeviceMapping	\N	\N
\.


--
-- Data for Name: biometric_device_mappings; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.biometric_device_mappings (id, device_id, terminal_user_id, employee_id, enrolled_at, created_at, updated_at, created_by, updated_by) FROM stdin;
1	1	1001	1	2026-09-23 11:34:12.349167	2026-09-23 11:34:12.450301	2026-09-23 11:34:12.450301	\N	\N
2	2	1001	1	2026-09-24 07:52:19.26484	2026-09-24 07:52:19.265978	2026-09-24 07:52:19.265978	\N	\N
3	3	1001	1	2026-09-24 08:18:35.381948	2026-09-24 08:18:35.382849	2026-09-24 08:18:35.382849	\N	\N
4	3	emp-1	1	2026-09-28 09:21:20.923737	2026-09-28 09:21:21.371486	2026-09-28 09:21:21.371486	\N	\N
5	3	emp-2	2	2026-09-28 09:32:57.309494	2026-09-28 09:32:57.312673	2026-09-28 09:32:57.312673	\N	\N
6	3	emp-3	3	2026-09-28 11:14:15.264889	2026-09-28 11:14:16.684552	2026-09-28 11:14:16.684552	\N	\N
7	3	emp-4	4	2026-09-29 13:19:31.025869	2026-09-29 13:19:31.349728	2026-09-29 13:19:31.349728	\N	\N
8	3	emp-5	5	2026-09-29 13:19:46.890348	2026-09-29 13:19:46.890851	2026-09-29 13:19:46.890851	\N	\N
9	3	emp-6	6	2026-09-29 13:20:42.865439	2026-09-29 13:20:42.865932	2026-09-29 13:20:42.865932	\N	\N
10	3	emp-7	7	2026-09-29 13:29:39.040522	2026-09-29 13:29:39.059177	2026-09-29 13:29:39.059177	\N	\N
11	3	emp-8	8	2026-09-29 17:17:41.694728	2026-09-29 17:17:41.721593	2026-09-29 17:17:41.721593	\N	\N
\.


--
-- Data for Name: biometric_devices; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.biometric_devices (id, created_at, updated_at, created_by, updated_by, branch_id, device_serial, device_token_hash, last_sync_at, status) FROM stdin;
1	2026-09-23 11:09:57.757716	2026-09-24 08:15:16.79111	\N	\N	1	SECUGEN-BRANCH1-001	$2a$10$TXTmWDyGLAlejpScbf69rupLN/cJa3qsHE1IbPlS2T4pNBskYs8zm	\N	INACTIVE
2	2026-09-24 07:48:17.449109	2026-09-24 08:15:52.957584	\N	\N	1	SECUGEN-BRANCH1-002	$2a$10$SdXVFjBs5kbrTcBGOaZFVu5a/wBdSRl.GoicIcmPV34myL3gHeSpO	\N	INACTIVE
3	2026-09-24 08:16:45.510962	2026-09-28 12:32:48.928119	\N	\N	1	SECUGEN-BRANCH1-003	$2a$10$bdO2d9.5HhJCmV4XG2rRHOnPO0AOb2q37KkDyMpzLXp33UeZr0qgu	2026-09-28 12:32:48.768677	ACTIVE
\.


--
-- Data for Name: biometric_sync_failures; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.biometric_sync_failures (id, created_at, event_id, device_serial, employee_id, reason) FROM stdin;
\.


--
-- Data for Name: biometric_templates; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.biometric_templates (id, created_at, updated_at, created_by, updated_by, employee_id, branch_id, template_data, finger_index, quality_score, enrolled_at) FROM stdin;
\.


--
-- Data for Name: branches; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.branches (id, created_at, updated_at, created_by, updated_by, name, code, address, phone, email, manager_id, status) FROM stdin;
1	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Head Office	HQ-001	\N	\N	\N	\N	ACTIVE
\.


--
-- Data for Name: companies; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.companies (id, created_at, updated_at, created_by, updated_by, name, logo_url, address, phone, email, website, kra_pin, shif_number, nssf_number, nita_number, currency, timezone) FROM stdin;
\.


--
-- Data for Name: company_settings; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.company_settings (id, created_at, updated_at, created_by, updated_by, company_id, vat_rate, default_working_hours_per_day, overtime_multiplier, weekend_days, payroll_period) FROM stdin;
\.


--
-- Data for Name: departments; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.departments (id, created_at, updated_at, created_by, updated_by, name, branch_id, head_of_department_id) FROM stdin;
\.


--
-- Data for Name: employee_documents; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.employee_documents (id, created_at, updated_at, created_by, updated_by, employee_id, document_type, file_url, file_name) FROM stdin;
\.


--
-- Data for Name: employees; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.employees (id, created_at, updated_at, created_by, updated_by, full_name, employee_number, passport_photo_url, national_id, date_of_birth, gender, marital_status, phone, email, address, kra_pin, shif_number, nssf_number, nita_number, branch_id, department_id, agent_id, "position", employment_type, basic_salary, date_hired, employment_status, bank_name, bank_account_number, emergency_contact_name, emergency_contact_phone) FROM stdin;
3	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Partel	EMP-1003	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	5000.00	\N	ACTIVE	\N	\N	\N	\N
2	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Tom Jacob	EMP-1002	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	4000.00	\N	ACTIVE	\N	\N	\N	\N
1	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Dickson Moseti	EMP-1001	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	30000.00	\N	ACTIVE	\N	\N	\N	\N
4	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Jonasi	EMP-1004	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	20000.00	\N	ACTIVE	\N	\N	\N	\N
5	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Hellen Kemunto	EMP-1005	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	50000.00	\N	ACTIVE	\N	\N	\N	\N
6	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Kevin Masanga	EMP-1006	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	70000.00	\N	ACTIVE	\N	\N	\N	\N
7	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Kasongo	EMP-1007	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	80000.00	\N	ACTIVE	\N	\N	\N	\N
8	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	Bianga	EMP-1008	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	\N	1	\N	\N	\N	PERMANENT	100000.00	\N	ACTIVE	\N	\N	\N	\N
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	baseline schema	SQL	V1__baseline_schema.sql	1109403657	hrpayroll_user	2026-09-20 07:59:48.003135	2170	t
2	2	fix audit logs details json type	SQL	V2__fix_audit_logs_details_json_type.sql	-1436759804	hrpayroll_user	2026-09-20 07:59:50.429078	29	t
3	4	seed staff and agent users	SQL	V4__seed_staff_and_agent_users.sql	1166886178	hrpayroll_user	2026-09-20 07:59:50.516791	30	t
4	5	seed branch and employee	SQL	V5__seed_branch_and_employee.sql	1697021902	hrpayroll_user	2026-09-20 07:59:50.629591	39	t
5	6	fix employee employment type	SQL	V6__fix_employee_employment_type.sql	-1435137240	hrpayroll_user	2026-09-20 07:59:50.766443	26	t
6	7	link agent user to agent record	SQL	V7__link_agent_user_to_agent_record.sql	636443006	hrpayroll_user	2026-09-20 07:59:50.831499	19	t
7	8	create biometric sync failures	SQL	V8__create_biometric_sync_failures.sql	-699467763	hrpayroll_user	2026-09-20 07:59:50.892286	117	t
8	9	create biometric device mappings	SQL	V9__create_biometric_device_mappings.sql	-1408187410	hrpayroll_user	2026-09-20 07:59:51.049191	261	t
9	10	attendance clock out event	SQL	V10__attendance_clock_out_event.sql	277184193	hrpayroll_user	2026-10-06 08:06:52.849519	211	t
\.


--
-- Data for Name: leave_requests; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.leave_requests (id, created_at, updated_at, created_by, updated_by, employee_id, leave_type_id, start_date, end_date, reason, status, approved_by_manager_id, approved_by_hr_id, rejection_reason) FROM stdin;
\.


--
-- Data for Name: leave_types; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.leave_types (id, created_at, updated_at, created_by, updated_by, name, default_entitlement_days, paid) FROM stdin;
\.


--
-- Data for Name: loan_repayments; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.loan_repayments (id, created_at, updated_at, created_by, updated_by, loan_id, payroll_run_id, amount_paid) FROM stdin;
\.


--
-- Data for Name: loans; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.loans (id, created_at, updated_at, created_by, updated_by, employee_id, principal_amount, interest_rate, installment_amount, outstanding_balance, request_date, approval_date, status) FROM stdin;
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.notifications (id, created_at, updated_at, created_by, updated_by, recipient_user_id, channel, type, message, read) FROM stdin;
\.


--
-- Data for Name: payroll_runs; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.payroll_runs (id, created_at, updated_at, created_by, updated_by, period_start, period_end, scope_branch_ids, scope_agent_ids, scope_department_ids, status, total_gross_payroll, total_net_payroll, initiated_by_user_id) FROM stdin;
\.


--
-- Data for Name: payslips; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.payslips (id, created_at, updated_at, created_by, updated_by, payroll_run_id, employee_id, basic_salary, house_allowance, transport_allowance, overtime_pay, bonus, other_allowances, gross_pay, shif_deduction, nssf_deduction, housing_levy_deduction, paye_deduction, nita_deduction, loan_deduction, other_deductions, net_pay) FROM stdin;
\.


--
-- Data for Name: shift_assignments; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.shift_assignments (id, created_at, updated_at, created_by, updated_by, shift_id, employee_id, department_id, branch_id) FROM stdin;
\.


--
-- Data for Name: shifts; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.shifts (id, created_at, updated_at, created_by, updated_by, name, type, start_time, end_time) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: hrpayroll_user
--

COPY public.users (id, created_at, updated_at, created_by, updated_by, username, password, role, display_name, employee_id, agent_id, branch_id, active, must_change_password) FROM stdin;
1	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	ADMIN	$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO	ADMIN	Administrator	\N	\N	\N	t	f
2	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	HR-MANAGER	$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO	HR_MANAGER	HR Manager	\N	\N	\N	t	f
3	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	PAYROLL-OFFICER	$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO	PAYROLL_OFFICER	Payroll Officer	\N	\N	\N	t	f
5	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	FINANCE-OFFICER	$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO	FINANCE_OFFICER	Finance Officer	\N	\N	\N	t	f
6	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	AUDITOR	$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO	AUDITOR	Auditor	\N	\N	\N	t	f
8	2026-09-20 07:59:50.663962	2026-09-20 07:59:50.663962	\N	\N	EMP-1001	$2b$10$e76DMykFVRjiV3mYhlxvGuxvLeNbEzY9gmgQddwb/OWmHi96sdrt.	EMPLOYEE	Test Employee	1	\N	\N	t	f
7	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	AGENT	$2b$10$7jbsLJnBYv156VEWnpcQH.S49Rm2nfE8uK9ulmlbqru1cZjrP5ZD.	AGENT	Test Agent	\N	1	\N	t	f
4	2026-09-20 07:59:50.552177	2026-09-20 07:59:50.552177	\N	\N	BRANCH-MANAGER	$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO	BRANCH_MANAGER	Branch Manager	\N	\N	1	t	f
\.


--
-- Name: agent_advances_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.agent_advances_id_seq', 1, false);


--
-- Name: agent_payroll_summaries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.agent_payroll_summaries_id_seq', 1, false);


--
-- Name: agents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.agents_id_seq', 1, true);


--
-- Name: attendance_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.attendance_id_seq', 1, true);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 16, true);


--
-- Name: biometric_device_mappings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.biometric_device_mappings_id_seq', 11, true);


--
-- Name: biometric_devices_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.biometric_devices_id_seq', 3, true);


--
-- Name: biometric_sync_failures_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.biometric_sync_failures_id_seq', 8940, true);


--
-- Name: biometric_templates_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.biometric_templates_id_seq', 1, false);


--
-- Name: branches_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.branches_id_seq', 1, true);


--
-- Name: companies_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.companies_id_seq', 1, false);


--
-- Name: company_settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.company_settings_id_seq', 1, false);


--
-- Name: departments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.departments_id_seq', 1, false);


--
-- Name: employee_documents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.employee_documents_id_seq', 1, false);


--
-- Name: employees_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.employees_id_seq', 1, true);


--
-- Name: leave_requests_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.leave_requests_id_seq', 1, false);


--
-- Name: leave_types_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.leave_types_id_seq', 1, false);


--
-- Name: loan_repayments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.loan_repayments_id_seq', 1, false);


--
-- Name: loans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.loans_id_seq', 1, false);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.notifications_id_seq', 1, false);


--
-- Name: payroll_runs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.payroll_runs_id_seq', 1, false);


--
-- Name: payslips_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.payslips_id_seq', 1, false);


--
-- Name: shift_assignments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.shift_assignments_id_seq', 1, false);


--
-- Name: shifts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.shifts_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: hrpayroll_user
--

SELECT pg_catalog.setval('public.users_id_seq', 8, true);


--
-- Name: agent_advances agent_advances_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.agent_advances
    ADD CONSTRAINT agent_advances_pkey PRIMARY KEY (id);


--
-- Name: agent_payroll_summaries agent_payroll_summaries_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.agent_payroll_summaries
    ADD CONSTRAINT agent_payroll_summaries_pkey PRIMARY KEY (id);


--
-- Name: agents agents_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.agents
    ADD CONSTRAINT agents_pkey PRIMARY KEY (id);


--
-- Name: attendance attendance_clock_out_event_id_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_clock_out_event_id_key UNIQUE (clock_out_event_id);


--
-- Name: attendance attendance_event_id_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_event_id_key UNIQUE (event_id);


--
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: biometric_device_mappings biometric_device_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_device_mappings
    ADD CONSTRAINT biometric_device_mappings_pkey PRIMARY KEY (id);


--
-- Name: biometric_devices biometric_devices_device_serial_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_devices
    ADD CONSTRAINT biometric_devices_device_serial_key UNIQUE (device_serial);


--
-- Name: biometric_devices biometric_devices_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_devices
    ADD CONSTRAINT biometric_devices_pkey PRIMARY KEY (id);


--
-- Name: biometric_sync_failures biometric_sync_failures_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_sync_failures
    ADD CONSTRAINT biometric_sync_failures_pkey PRIMARY KEY (id);


--
-- Name: biometric_templates biometric_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_templates
    ADD CONSTRAINT biometric_templates_pkey PRIMARY KEY (id);


--
-- Name: branches branches_code_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.branches
    ADD CONSTRAINT branches_code_key UNIQUE (code);


--
-- Name: branches branches_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.branches
    ADD CONSTRAINT branches_pkey PRIMARY KEY (id);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: company_settings company_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.company_settings
    ADD CONSTRAINT company_settings_pkey PRIMARY KEY (id);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: employee_documents employee_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.employee_documents
    ADD CONSTRAINT employee_documents_pkey PRIMARY KEY (id);


--
-- Name: employees employees_employee_number_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_employee_number_key UNIQUE (employee_number);


--
-- Name: employees employees_national_id_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_national_id_key UNIQUE (national_id);


--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: leave_requests leave_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.leave_requests
    ADD CONSTRAINT leave_requests_pkey PRIMARY KEY (id);


--
-- Name: leave_types leave_types_name_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.leave_types
    ADD CONSTRAINT leave_types_name_key UNIQUE (name);


--
-- Name: leave_types leave_types_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.leave_types
    ADD CONSTRAINT leave_types_pkey PRIMARY KEY (id);


--
-- Name: loan_repayments loan_repayments_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.loan_repayments
    ADD CONSTRAINT loan_repayments_pkey PRIMARY KEY (id);


--
-- Name: loans loans_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.loans
    ADD CONSTRAINT loans_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: payroll_runs payroll_runs_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.payroll_runs
    ADD CONSTRAINT payroll_runs_pkey PRIMARY KEY (id);


--
-- Name: payslips payslips_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_pkey PRIMARY KEY (id);


--
-- Name: shift_assignments shift_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.shift_assignments
    ADD CONSTRAINT shift_assignments_pkey PRIMARY KEY (id);


--
-- Name: shifts shifts_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.shifts
    ADD CONSTRAINT shifts_pkey PRIMARY KEY (id);


--
-- Name: biometric_device_mappings uk_device_terminal_user; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_device_mappings
    ADD CONSTRAINT uk_device_terminal_user UNIQUE (device_id, terminal_user_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_agent_advances_agent_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_agent_advances_agent_id ON public.agent_advances USING btree (agent_id);


--
-- Name: idx_attendance_attendance_date; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_attendance_attendance_date ON public.attendance USING btree (attendance_date);


--
-- Name: idx_attendance_branch_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_attendance_branch_id ON public.attendance USING btree (branch_id);


--
-- Name: idx_attendance_clock_in; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_attendance_clock_in ON public.attendance USING btree (clock_in);


--
-- Name: idx_attendance_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_attendance_employee_id ON public.attendance USING btree (employee_id);


--
-- Name: idx_biometric_sync_failures_created_at; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_biometric_sync_failures_created_at ON public.biometric_sync_failures USING btree (created_at);


--
-- Name: idx_biometric_sync_failures_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_biometric_sync_failures_employee_id ON public.biometric_sync_failures USING btree (employee_id);


--
-- Name: idx_biometric_templates_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_biometric_templates_employee_id ON public.biometric_templates USING btree (employee_id);


--
-- Name: idx_departments_branch_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_departments_branch_id ON public.departments USING btree (branch_id);


--
-- Name: idx_device_mappings_device_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_device_mappings_device_id ON public.biometric_device_mappings USING btree (device_id);


--
-- Name: idx_device_mappings_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_device_mappings_employee_id ON public.biometric_device_mappings USING btree (employee_id);


--
-- Name: idx_employees_agent_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_employees_agent_id ON public.employees USING btree (agent_id);


--
-- Name: idx_employees_branch_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_employees_branch_id ON public.employees USING btree (branch_id);


--
-- Name: idx_leave_requests_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_leave_requests_employee_id ON public.leave_requests USING btree (employee_id);


--
-- Name: idx_loan_repayments_loan_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_loan_repayments_loan_id ON public.loan_repayments USING btree (loan_id);


--
-- Name: idx_loans_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_loans_employee_id ON public.loans USING btree (employee_id);


--
-- Name: idx_notifications_recipient_user_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_notifications_recipient_user_id ON public.notifications USING btree (recipient_user_id);


--
-- Name: idx_payslips_employee_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_payslips_employee_id ON public.payslips USING btree (employee_id);


--
-- Name: idx_payslips_payroll_run_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_payslips_payroll_run_id ON public.payslips USING btree (payroll_run_id);


--
-- Name: idx_shift_assignments_shift_id; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_shift_assignments_shift_id ON public.shift_assignments USING btree (shift_id);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: hrpayroll_user
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: biometric_device_mappings biometric_device_mappings_device_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_device_mappings
    ADD CONSTRAINT biometric_device_mappings_device_id_fkey FOREIGN KEY (device_id) REFERENCES public.biometric_devices(id);


--
-- Name: biometric_device_mappings biometric_device_mappings_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: hrpayroll_user
--

ALTER TABLE ONLY public.biometric_device_mappings
    ADD CONSTRAINT biometric_device_mappings_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- PostgreSQL database dump complete
--

\unrestrict 02WGMA1bnopmASYqdvem8Qz4lPZzbFwsHIZspPadEcXfnQmJxvjY1PQIIo2AWFU

