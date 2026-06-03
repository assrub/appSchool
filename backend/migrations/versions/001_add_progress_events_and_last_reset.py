"""add progress_events table and last_reset_at column

Revision ID: 001
Revises:
Create Date: 2026-06-03
"""
from alembic import op
import sqlalchemy as sa

revision = '001'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column('progress', sa.Column('last_reset_at', sa.DateTime(), nullable=True))

    op.create_table(
        'progress_events',
        sa.Column('id', sa.Integer(), autoincrement=True, nullable=False),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('app_users.id'), nullable=False),
        sa.Column('event_type', sa.String(30), nullable=False),
        sa.Column('topic_id', sa.String(100), nullable=False),
        sa.Column('unit_id', sa.String(100), nullable=True),
        sa.Column('block_index', sa.Integer(), nullable=True),
        sa.Column('event_data', sa.JSON(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
        sa.PrimaryKeyConstraint('id'),
    )
    op.create_index('ix_progress_events_user', 'progress_events', ['user_id'])
    op.create_index('ix_progress_events_user_topic_unit', 'progress_events', ['user_id', 'topic_id', 'unit_id'])


def downgrade() -> None:
    op.drop_index('ix_progress_events_user_topic_unit', table_name='progress_events')
    op.drop_index('ix_progress_events_user', table_name='progress_events')
    op.drop_table('progress_events')
    op.drop_column('progress', 'last_reset_at')
