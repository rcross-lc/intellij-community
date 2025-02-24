// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

use std::{env, process};
use std::error::Error;
use std::io::ErrorKind;

#[cfg(target_family = "unix")]
use std::os::unix::process::CommandExt;

mod logger;

type Result<T> = std::result::Result<T, Box<dyn Error>>;

fn main() {
    let args = env::args().collect::<Vec<String>>();
    if args.len() < 4 {
        println!("usage: {} <pid> (n_args args ...)+", args[0]);
        process::exit(1);
    }

    let mut log = logger::Logger::init();
    log.info(&format!("args: {:?}", args));
    log.info(&format!("CWD: {:?}", env::current_dir()));

    if let Err(e) = process_args(&args, &mut log) {
        log.error(&format!("{}", e));
        process::exit(1);
    }
}

fn process_args(args: &Vec<String>, log: &mut logger::Logger) -> Result<()> {
    let pid = args[1].parse::<i32>()?;
    let mut commands = Vec::<&[String]>::new();
    let mut arg_idx = 2;
    while arg_idx < args.len() {
        let arg_n = args[arg_idx].parse::<usize>()?;
        if arg_n == 0 || arg_idx + arg_n >= args.len() {
            return Err(Box::new(std::io::Error::new(ErrorKind::InvalidInput, format!("unexpected '{}' at {}", arg_n, arg_idx))));
        }
        arg_idx += 1;
        commands.push(&args[arg_idx .. arg_idx + arg_n]);
        arg_idx += arg_n;
    }

    log.info(&format!("waiting for {}", pid));
    wait_for_process_exit(pid);

    let last_idx = commands.len() - 1;
    for (i, command) in commands.iter().enumerate() {
        if i != last_idx {
            run_command(command, log)?;
        } else {
            exec_command(command, log)?;
        }
    }

    Ok(())
}

#[cfg(target_os = "windows")]
fn wait_for_process_exit(pid: i32) {
    todo!("Windows implementation isn't there yet")
}

#[cfg(target_family = "unix")]
fn wait_for_process_exit(pid: i32) {
    let delay = std::time::Duration::from_millis(100);
    while unsafe { libc::getppid() } == pid {
        std::thread::sleep(delay);
    }
}

fn run_command(command: &[String], log: &mut logger::Logger) -> Result<()> {
    log.info(&format!("running: {:?}", command));
    let output = process::Command::new(&command[0]).args(&command[1..]).output()?;
    log.info(&format!("finished, {}", output.status));
    log.info(&format!("stdout: {}", format_output(&output.stdout)));
    log.info(&format!("stderr: {}", format_output(&output.stderr)));
    Ok(())
}

fn format_output(bytes: &[u8]) -> String {
    let full = String::from_utf8_lossy(bytes);
    let trimmed = full.trim();
    if trimmed.is_empty() { String::from("-") } else { String::from("\n") + trimmed }
}

#[cfg(target_os = "windows")]
fn exec_command(command: &[String], log: &mut logger::Logger) -> Result<()> {
    todo!("Windows implementation isn't there yet")
}

#[cfg(target_family = "unix")]
fn exec_command(command: &[String], log: &mut logger::Logger) -> Result<()> {
    log.info(&format!("exec-ing: {:?}", command));
    Err(Box::new(process::Command::new(&command[0]).args(&command[1..]).exec()))
}
