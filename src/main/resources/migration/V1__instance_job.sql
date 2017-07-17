CREATE TABLE running_jobs (
    group_name          TEXT NOT NULL,
    node_id             TEXT NOT NULL,
    job_id              TEXT NOT NULL,
    job_lock            TEXT NOT NULL,
    retries             INTEGER NOT NULL,
    meta_data           JSONB,
    start_time          TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY(job_id),
    UNIQUE(job_lock)
);

CREATE TABLE queued_jobs (
    group_name          TEXT NOT NULL,
    node_id             TEXT NOT NULL,
    job_id              TEXT NOT NULL,
    job_lock            TEXT NOT NULL,
    retries             INTEGER NOT NULL,
    meta_data           JSONB,
    queuing_time        TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY(job_id)
);

CREATE TABLE finished_jobs (
    group_name          TEXT NOT NULL,
    node_id             TEXT NOT NULL,
    job_id              TEXT NOT NULL,
    job_lock            TEXT NOT NULL,
    retries             INTEGER NOT NULL,
    meta_data           JSONB,
    finish_time         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE current_nodes (
    group_name          TEXT NOT NULL,
    node_id             TEXT NOT NULL,
    last_updated        TIMESTAMP WITH TIME ZONE NOT NULL,
    start_time          TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY(group_name, node_id)
);

