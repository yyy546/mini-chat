const LEVELS = { DEBUG: 0, INFO: 1, WARN: 2, ERROR: 3, SILENT: 4 }

const currentLevel = import.meta.env.DEV ? LEVELS.DEBUG : LEVELS.WARN

const noop = () => {}

const logger = {
  debug: currentLevel <= LEVELS.DEBUG ? console.debug.bind(console, '[DEBUG]') : noop,
  info: currentLevel <= LEVELS.INFO ? console.info.bind(console, '[INFO]') : noop,
  warn: currentLevel <= LEVELS.WARN ? console.warn.bind(console, '[WARN]') : noop,
  error: currentLevel <= LEVELS.ERROR ? console.error.bind(console, '[ERROR]') : noop
}

export default logger
