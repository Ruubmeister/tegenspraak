import { MAX_MESSAGE_LENGTH } from './chatDrafts'

/** From this share of the limit the counter stands out, so a long message does not hit the limit unnoticed. */
const WARNING_RATIO = 0.9

interface MessageLengthCounterProps {
  id: string
  length: number
}

/** Shows how much of the message limit is used, and how much too long the message is. */
export function MessageLengthCounter({ id, length }: MessageLengthCounterProps) {
  const isTooLong = length > MAX_MESSAGE_LENGTH
  const level = isTooLong ? 'over' : length >= MAX_MESSAGE_LENGTH * WARNING_RATIO ? 'near' : 'ok'
  return (
    <span id={id} className={`message-length ${level}`}>
      {length} / {MAX_MESSAGE_LENGTH} tekens
    </span>
  )
}
